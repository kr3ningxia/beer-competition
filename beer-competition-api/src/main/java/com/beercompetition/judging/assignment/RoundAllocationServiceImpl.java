package com.beercompetition.judging.assignment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.billing.beercoin.BeerCoinSettlementService;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.pojo.dto.FirstRoundCreateRequest;
import com.beercompetition.pojo.dto.NextRoundCreateRequest;
import com.beercompetition.pojo.dto.RoundAllocationRequest;
import com.beercompetition.pojo.dto.RoundTableAllocationRequest;
import com.beercompetition.pojo.dto.RoundTableMemberAllocationRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundCreationStrategy;
import com.beercompetition.pojo.enums.RoundEntryStatus;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import com.beercompetition.service.impl.round.RoundValidationPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import static com.beercompetition.service.impl.round.RoundConstants.CATEGORY_MODE_CATEGORY;
import static com.beercompetition.service.impl.round.RoundConstants.CATEGORY_MODE_EMPTY;
import static com.beercompetition.service.impl.round.RoundConstants.CATEGORY_MODE_MIXED;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_FALSE;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_TRUE;
import com.beercompetition.judging.assignment.RoundAllocationService;

/**
 * 在事务内创建轮次并持久化分桌、评委和候选酒款分配。
 */
@Service
@RequiredArgsConstructor
public class RoundAllocationServiceImpl implements RoundAllocationService {

    private final JudgeTableMapper judgeTableMapper;

    private final JudgeAssignmentMapper judgeAssignmentMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMapper roundTableMapper;

    private final RoundTableMemberMapper roundTableMemberMapper;

    private final RoundTableEntryMapper roundTableEntryMapper;

    private final RoundResultMapper roundResultMapper;

    private final RoundQuerySupport roundQuerySupport;

    private final RoundValidationPolicy roundValidationPolicy;

    private final RoundCandidateSyncService roundCandidateSyncService;

    private final BeerCoinSettlementService beerCoinSettlementService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFirstRound(Long competitionId, FirstRoundCreateRequest request) {
        // 1) 查询比赛、基础桌和可分配酒款
        Competition competition = roundQuerySupport.requireCompetition(competitionId);
        CompetitionStatus competitionStatus = roundValidationPolicy.parseCompetitionStatus(competition);
        if (!Set.of(
                CompetitionStatus.DRAFT,
                CompetitionStatus.REGISTRATION_OPEN,
                CompetitionStatus.REGISTRATION_CLOSED,
                CompetitionStatus.JUDGING_PREP
        ).contains(competitionStatus)) {
            throw new BaseException("评审已开始，不能再创建首轮编排");
        }
        // 首轮创建和后续分桌都属于正式评审准备动作，租户赛事必须先完成最终结算。
        // 平台赛事由结算服务按赛事归属自动跳过该门槛。
        beerCoinSettlementService.requireJudgingSettlementCompleted(competitionId);
        if (competitionRoundMapper.selectCount(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .eq(CompetitionRound::getRoundNo, 1)) > 0) {
            throw new BaseException("首轮已经创建");
        }
        List<JudgeTable> baseTables = roundQuerySupport.listBaseTables(competitionId);
        if (baseTables.isEmpty()) {
            throw new BaseException("请先配置基础评审桌");
        }
        List<JudgeAssignment> assignments = roundQuerySupport.listBaseAssignments(competitionId);
        boolean hasTablePayload = request.getTables() != null && !request.getTables().isEmpty();
        if (!hasTablePayload) {
            validateBaseCaptains(baseTables, assignments);
            List<BeerEntry> entries = roundQuerySupport.listStoredEntries(competitionId);
            if (entries.isEmpty()) {
                throw new BaseException("没有可分配的已入库酒款");
            }
        }

        // 2) 创建首轮
        CompetitionRound round = CompetitionRound.builder()
                .competitionId(competitionId)
                .roundNo(1)
                .roundName("首轮")
                .roundType(RoundType.SCORE.name())
                .status(RoundStatus.DRAFT.name())
                .sortOrder(1)
                .build();
        competitionRoundMapper.insert(round);
        if (hasTablePayload) {
            RoundAllocationRequest allocation = new RoundAllocationRequest();
            allocation.setTables(request.getTables());
            roundValidationPolicy.validateAllocationRequest(round, allocation, false);
            saveAllocationTables(competitionId, round, allocation);
            return;
        }

        // 3) 没有传入酒款草稿时，只按基础桌生成空的首轮桌
        Map<Long, List<JudgeAssignment>> assignmentsByTable = assignments.stream().collect(Collectors.groupingBy(JudgeAssignment::getTableId));
        List<RoundTable> roundTables = new ArrayList<>();
        for (int index = 0; index < baseTables.size(); index++) {
            JudgeTable baseTable = baseTables.get(index);
            Long captainId = roundQuerySupport.findCaptainId(assignmentsByTable.getOrDefault(baseTable.getId(), List.of()));
            RoundTable table = RoundTable.builder()
                    .competitionId(competitionId)
                    .roundId(round.getId())
                    .tableName(baseTable.getTableName())
                    .captainJudgeId(captainId)
                    .categoryMode(CATEGORY_MODE_EMPTY)
                    .targetCount(request.getDefaultTargetCount())
                    .targetMode(RoundTargetMode.ADVANCE_COUNT.name())
                    .status(RoundStatus.DRAFT.name())
                    .sortOrder(index)
                    .build();
            roundTableMapper.insert(table);
            roundTables.add(table);
            insertMembers(table, assignmentsByTable.getOrDefault(baseTable.getId(), List.of()), true);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoundAllocation(Long competitionId, Long roundId, RoundAllocationRequest request) {
        // 1) 查询轮次并校验编辑状态
        Competition competition = roundQuerySupport.requireCompetition(competitionId);
        CompetitionRound round = roundQuerySupport.requireRound(competitionId, roundId);
        beerCoinSettlementService.requireJudgingSettlementCompleted(competitionId);
        roundValidationPolicy.validateCompetitionStageForRoundAllocation(competition, round);
        if (!RoundStatus.DRAFT.name().equals(round.getStatus())) {
            throw new BaseException("只有草稿轮次可以保存编排");
        }
        roundValidationPolicy.validateAllocationRequest(round, request, false);

        // 2) 清理旧编排数据
        List<RoundTable> oldTables = roundQuerySupport.listRoundTables(roundId);
        List<Long> oldTableIds = oldTables.stream().map(RoundTable::getId).toList();
        if (!oldTableIds.isEmpty()) {
            roundResultMapper.delete(new LambdaQueryWrapper<RoundResult>().in(RoundResult::getRoundTableId, oldTableIds));
            roundTableMemberMapper.delete(new LambdaQueryWrapper<RoundTableMember>().in(RoundTableMember::getRoundTableId, oldTableIds));
            roundTableEntryMapper.delete(new LambdaQueryWrapper<RoundTableEntry>().in(RoundTableEntry::getRoundTableId, oldTableIds));
            roundTableMapper.delete(new LambdaQueryWrapper<RoundTable>().in(RoundTable::getId, oldTableIds));
        }

        // 3) 全量写入新的轮次桌、桌长和酒款
        saveAllocationTables(competitionId, round, request);
    }

    private void saveAllocationTables(Long competitionId, CompetitionRound round, RoundAllocationRequest request) {
        Map<String, JudgeAccount> captainMap = roundQuerySupport.loadJudgeByPublicIds(request.getTables().stream()
                .map(RoundTableAllocationRequest::getCaptainPublicId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));
        Map<String, JudgeAccount> scoreMemberMap = roundQuerySupport.loadJudgeByPublicIds(request.getTables().stream()
                .flatMap(table -> safeList(table.getMembers()).stream())
                .map(RoundTableMemberAllocationRequest::getJudgePublicId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));
        Map<String, JudgeAccount> participantMap = roundQuerySupport.loadJudgeByPublicIds(request.getTables().stream()
                .flatMap(table -> safeList(table.getParticipantPublicIds()).stream())
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));
        Map<String, BeerEntry> entryMap = roundQuerySupport.loadEntryByUuids(competitionId, request.getTables().stream()
                .flatMap(table -> safeList(table.getEntryUuids()).stream())
                .collect(Collectors.toSet()));
        roundValidationPolicy.validateAllocationReferences(round, request, captainMap, scoreMemberMap, participantMap, entryMap);
        roundValidationPolicy.validateEntrySource(round, entryMap.values().stream().toList());
        int tableIndex = 0;
        for (RoundTableAllocationRequest item : request.getTables()) {
            JudgeAccount captain = captainMap.get(item.getCaptainPublicId());
            Long categoryId = resolveCategoryId(item, entryMap);
            RoundTable table = RoundTable.builder()
                    .competitionId(competitionId)
                    .roundId(round.getId())
                    .tableName(item.getName().trim())
                    .captainJudgeId(captain == null ? null : captain.getId())
                    .categoryId(categoryId)
                    .categoryMode(resolveCategoryMode(item, entryMap))
                    .targetCount(item.getTargetCount())
                    .targetMode(roundValidationPolicy.resolveTargetMode(round, item.getTargetMode()))
                    .status(RoundStatus.DRAFT.name())
                    .sortOrder(item.getSortOrder() == null ? tableIndex : item.getSortOrder())
                    .build();
            roundTableMapper.insert(table);
            if (captain != null) {
                insertCaptainMember(table, captain.getId());
            }
            if (RoundType.SCORE.name().equals(round.getRoundType())) {
                insertScoreRoundMembers(table, item.getMembers(), scoreMemberMap, captain == null ? null : captain.getId(), item.getName().trim());
            } else {
                insertRankingParticipants(table, item.getParticipantPublicIds(), participantMap, captain == null ? null : captain.getId());
            }
            int entryIndex = 0;
            for (String uuid : safeList(item.getEntryUuids()).stream().filter(StringUtils::hasText).toList()) {
                BeerEntry entry = entryMap.get(uuid);
                roundTableEntryMapper.insert(RoundTableEntry.builder()
                        .competitionId(competitionId)
                        .roundId(round.getId())
                        .roundTableId(table.getId())
                        .beerEntryId(entry.getId())
                        .sourceRoundTableId(resolveSourceRoundTableId(round, entry.getId()))
                        .status(RoundEntryStatus.ASSIGNED.name())
                        .sortOrder(entryIndex++)
                        .build());
            }
            tableIndex++;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraftRound(Long competitionId, Long roundId) {
        // 1) 查询轮次并限制只能删除草稿排序轮
        roundQuerySupport.requireCompetition(competitionId);
        CompetitionRound round = roundQuerySupport.requireRound(competitionId, roundId);
        if (!RoundType.RANKING.name().equals(round.getRoundType())) {
            throw new BaseException("只能删除排序轮草稿");
        }
        if (!RoundStatus.DRAFT.name().equals(round.getStatus())) {
            throw new BaseException("只有草稿轮次可以删除");
        }

        // 2) 清理草稿轮关联数据
        List<Long> tableIds = roundQuerySupport.listRoundTables(roundId).stream()
                .map(RoundTable::getId)
                .toList();
        roundResultMapper.delete(new LambdaQueryWrapper<RoundResult>().eq(RoundResult::getRoundId, roundId));
        roundTableEntryMapper.delete(new LambdaQueryWrapper<RoundTableEntry>().eq(RoundTableEntry::getRoundId, roundId));
        if (!tableIds.isEmpty()) {
            roundTableMemberMapper.delete(new LambdaQueryWrapper<RoundTableMember>().in(RoundTableMember::getRoundTableId, tableIds));
            roundTableMapper.delete(new LambdaQueryWrapper<RoundTable>().in(RoundTable::getId, tableIds));
        }
        competitionRoundMapper.deleteById(roundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNextRound(Long competitionId, NextRoundCreateRequest request) {
        // 1) 查询来源轮次和晋级候选
        Competition competition = roundQuerySupport.requireCompetition(competitionId);
        beerCoinSettlementService.requireJudgingSettlementCompleted(competitionId);
        if (resolveCompetitionType(competition) == CompetitionType.FEEDBACK_ONLY) {
            throw new BaseException("风格对齐会不创建后续轮");
        }
        CompetitionStatus competitionStatus = roundValidationPolicy.parseCompetitionStatus(competition);
        RoundTargetMode targetMode = RoundTargetMode.of(request.getTargetMode());
        boolean creatingChampionRound = targetMode == RoundTargetMode.CHAMPION && competitionStatus == CompetitionStatus.RESULT_CONFIRMING;
        if (competitionStatus != CompetitionStatus.JUDGING && !creatingChampionRound) {
            throw new BaseException("只有评审中的比赛可以创建后续轮次");
        }
        roundValidationPolicy.validateTargetCountForMode(request.getRoundName(), targetMode.name(), request.getTargetCount());
        CompetitionRound sourceRound = roundQuerySupport.requireRound(competitionId, request.getSourceRoundId());
        if (isTerminalRound(sourceRound)) {
            throw new BaseException("决赛轮已是最后一轮，不能继续创建轮次");
        }
        boolean sourceLocked = RoundStatus.LOCKED.name().equals(sourceRound.getStatus());
        boolean sourceReady = roundCandidateSyncService.isSourceReady(sourceRound);
        if (sourceLocked) {
            roundValidationPolicy.validateSourceIsLatestLockedRound(competitionId, sourceRound);
        }
        if (competitionRoundMapper.selectCount(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .eq(CompetitionRound::getSourceRoundId, sourceRound.getId())) > 0) {
            throw new BaseException("已基于该轮次创建过下一轮");
        }
        List<RoundResult> candidates = roundQuerySupport.filterCandidatesForTargetMode(
                roundQuerySupport.listSubmittedCandidateResults(sourceRound.getId()), targetMode.name());
        if (sourceReady && candidates.isEmpty()) {
            throw new BaseException("上一轮没有可用于创建下一轮的候选酒款");
        }
        int nextRoundNo = roundQuerySupport.listRounds(competitionId).stream().mapToInt(CompetitionRound::getRoundNo).max().orElse(1) + 1;
        RoundCreationStrategy strategy = RoundCreationStrategy.of(request.getStrategy());

        // 2) 创建后续轮与草稿桌
        CompetitionRound round = CompetitionRound.builder()
                .competitionId(competitionId)
                .roundNo(nextRoundNo)
                .roundName(request.getRoundName().trim())
                .roundType(RoundType.RANKING.name())
                .sourceRoundId(sourceRound.getId())
                .status(RoundStatus.DRAFT.name())
                .sortOrder(nextRoundNo)
                .build();
        competitionRoundMapper.insert(round);
        List<JudgeAccount> captains = resolveCaptains(request.getCaptainPublicIds(), request.getTableCount());
        List<RoundTable> tables = new ArrayList<>();
        for (int index = 0; index < request.getTableCount(); index++) {
            JudgeAccount captain = index < captains.size() ? captains.get(index) : null;
            RoundTable table = RoundTable.builder()
                    .competitionId(competitionId)
                    .roundId(round.getId())
                    .tableName(resolveNextRoundTableName(nextRoundNo, index, request.getTableCount()))
                    .captainJudgeId(captain == null ? null : captain.getId())
                    .categoryMode(CATEGORY_MODE_EMPTY)
                    .targetCount(request.getTargetCount())
                    .targetMode(targetMode.name())
                    .status(RoundStatus.DRAFT.name())
                    .sortOrder(index)
                    .build();
            roundTableMapper.insert(table);
            if (captain != null) {
                insertCaptainMember(table, captain.getId());
            }
            tables.add(table);
        }

        // 3) 按策略写入候选酒款分配
        if (sourceLocked && strategy != RoundCreationStrategy.MANUAL) {
            for (int index = 0; index < candidates.size(); index++) {
                RoundTable table = tables.get(resolveCandidateTableIndex(strategy, index, tables.size()));
                RoundResult source = candidates.get(index);
                roundTableEntryMapper.insert(RoundTableEntry.builder()
                        .competitionId(competitionId)
                        .roundId(round.getId())
                        .roundTableId(table.getId())
                        .beerEntryId(source.getBeerEntryId())
                        .sourceRoundTableId(source.getRoundTableId())
                        .status(RoundEntryStatus.ASSIGNED.name())
                        .sortOrder(index)
                        .build());
            }
        }
        roundCandidateSyncService.syncDraftRound(competitionId, round.getId());
    }

    private Long resolveCategoryId(RoundTableAllocationRequest table, Map<String, BeerEntry> entryMap) {
        List<Long> categoryIds = safeList(table.getEntryUuids()).stream()
                .map(entryMap::get)
                .filter(Objects::nonNull)
                .map(BeerEntry::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return categoryIds.size() == 1 ? categoryIds.get(0) : null;
    }

    private String resolveCategoryMode(RoundTableAllocationRequest table, Map<String, BeerEntry> entryMap) {
        List<Long> categoryIds = safeList(table.getEntryUuids()).stream()
                .map(entryMap::get)
                .filter(Objects::nonNull)
                .map(BeerEntry::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return CATEGORY_MODE_EMPTY;
        }
        if (categoryIds.size() == 1) {
            return CATEGORY_MODE_CATEGORY;
        }
        return CATEGORY_MODE_MIXED;
    }

    private String resolveCategoryMode(RoundTable table) {
        if (table.getCategoryId() != null) {
            return CATEGORY_MODE_CATEGORY;
        }
        if (StringUtils.hasText(table.getCategoryMode()) && CATEGORY_MODE_MIXED.equals(table.getCategoryMode())) {
            return CATEGORY_MODE_MIXED;
        }
        return CATEGORY_MODE_EMPTY;
    }

    private void insertMembers(RoundTable table, List<JudgeAssignment> assignments, boolean requireTasks) {
        for (JudgeAssignment assignment : assignments) {
            boolean taskRequired = requireTasks || JudgeRoleType.CAPTAIN.name().equals(assignment.getRole());
            roundTableMemberMapper.insert(RoundTableMember.builder()
                    .roundTableId(table.getId())
                    .judgeAccountId(assignment.getJudgeAccountId())
                    .role(assignment.getRole())
                    .systemTaskRequired(taskRequired ? FLAG_TRUE : FLAG_FALSE)
                    .build());
        }
    }

    private void insertCaptainMember(RoundTable table, Long captainJudgeId) {
        if (captainJudgeId == null) {
            return;
        }
        ensureJudgeAssignment(table, table.getTableName(), captainJudgeId, JudgeRoleType.CAPTAIN.name());
        roundTableMemberMapper.insert(RoundTableMember.builder()
                .roundTableId(table.getId())
                .judgeAccountId(captainJudgeId)
                .role(JudgeRoleType.CAPTAIN.name())
                .systemTaskRequired(FLAG_TRUE)
                .build());
    }

    private void insertRankingParticipants(RoundTable table,
                                           List<String> participantPublicIds,
                                           Map<String, JudgeAccount> participantMap,
                                           Long captainJudgeId) {
        safeList(participantPublicIds).stream()
                .filter(StringUtils::hasText)
                .map(participantMap::get)
                .filter(Objects::nonNull)
                .filter(judge -> !judge.getId().equals(captainJudgeId))
                .forEach(judge -> {
                    ensureJudgeAssignment(table, table.getTableName(), judge.getId(), JudgeRoleType.PROFESSIONAL.name());
                    roundTableMemberMapper.insert(RoundTableMember.builder()
                            .roundTableId(table.getId())
                            .judgeAccountId(judge.getId())
                            .role(JudgeRoleType.PROFESSIONAL.name())
                            .systemTaskRequired(FLAG_FALSE)
                            .build());
                });
    }

    private void insertScoreRoundMembers(RoundTable table,
                                         List<RoundTableMemberAllocationRequest> members,
                                         Map<String, JudgeAccount> memberMap,
                                         Long captainJudgeId,
                                         String tableName) {
        if (members == null) {
            insertScoreRoundMembersFromBaseTable(table, tableName);
            return;
        }
        List<RoundTableMemberAllocationRequest> normalizedMembers = safeList(members).stream()
                .filter(member -> member != null && StringUtils.hasText(member.getJudgePublicId()))
                .toList();
        if (normalizedMembers.isEmpty()) {
            return;
        }
        Set<Long> insertedIds = new HashSet<>();
        if (captainJudgeId != null) {
            insertedIds.add(captainJudgeId);
        }
        for (RoundTableMemberAllocationRequest item : normalizedMembers) {
            JudgeAccount judge = memberMap.get(item.getJudgePublicId());
            if (judge == null || !insertedIds.add(judge.getId())) {
                continue;
            }
            String memberRole = resolveScoreMemberRole(item.getRole());
            ensureJudgeAssignment(table, tableName, judge.getId(), memberRole);
            roundTableMemberMapper.insert(RoundTableMember.builder()
                    .roundTableId(table.getId())
                    .judgeAccountId(judge.getId())
                    .role(memberRole)
                    .systemTaskRequired(FLAG_TRUE)
                    .build());
        }
    }

    private String resolveScoreMemberRole(String role) {
        if (JudgeRoleType.CROSS.name().equals(role)) {
            return JudgeRoleType.CROSS.name();
        }
        return JudgeRoleType.PROFESSIONAL.name();
    }

    private void ensureJudgeAssignment(RoundTable table, String tableName, Long judgeId, String role) {
        if (table == null || judgeId == null || !StringUtils.hasText(role)) {
            return;
        }
        JudgeAssignment existing = judgeAssignmentMapper.selectOne(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, table.getCompetitionId())
                .eq(JudgeAssignment::getJudgeAccountId, judgeId)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        JudgeTable baseTable = ensureJudgeTable(table.getCompetitionId(), tableName, table.getSortOrder());
        judgeAssignmentMapper.insert(JudgeAssignment.builder()
                .competitionId(table.getCompetitionId())
                .tableId(baseTable.getId())
                .judgeAccountId(judgeId)
                .role(role)
                .build());
    }

    private JudgeTable ensureJudgeTable(Long competitionId, String tableName, Integer sortOrder) {
        String normalizedName = StringUtils.hasText(tableName) ? tableName.trim() : "评审桌";
        JudgeTable table = judgeTableMapper.selectOne(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, competitionId)
                .eq(JudgeTable::getTableName, normalizedName)
                .last("LIMIT 1"));
        if (table != null) {
            return table;
        }
        JudgeTable created = JudgeTable.builder()
                .competitionId(competitionId)
                .tableName(normalizedName)
                .sortOrder(sortOrder == null ? 0 : sortOrder)
                .build();
        judgeTableMapper.insert(created);
        return created;
    }

    private void insertScoreRoundMembersFromBaseTable(RoundTable table, String tableName) {
        JudgeTable baseTable = judgeTableMapper.selectOne(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, table.getCompetitionId())
                .eq(JudgeTable::getTableName, tableName));
        if (baseTable == null) {
            return;
        }
        List<JudgeAssignment> assignments = judgeAssignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, table.getCompetitionId())
                .eq(JudgeAssignment::getTableId, baseTable.getId()));
        Set<Long> existingIds = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                        .eq(RoundTableMember::getRoundTableId, table.getId()))
                .stream()
                .map(RoundTableMember::getJudgeAccountId)
                .collect(Collectors.toSet());
        assignments.stream()
                .filter(assignment -> !JudgeRoleType.CAPTAIN.name().equals(assignment.getRole()))
                .filter(assignment -> !existingIds.contains(assignment.getJudgeAccountId()))
                .forEach(assignment -> roundTableMemberMapper.insert(RoundTableMember.builder()
                        .roundTableId(table.getId())
                        .judgeAccountId(assignment.getJudgeAccountId())
                        .role(assignment.getRole())
                        .systemTaskRequired(FLAG_TRUE)
                        .build()));
    }

    private Long resolveSourceRoundTableId(CompetitionRound round, Long beerEntryId) {
        if (round.getSourceRoundId() == null) {
            return null;
        }
        RoundResult source = roundResultMapper.selectOne(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundId, round.getSourceRoundId())
                .eq(RoundResult::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
        return source == null ? null : source.getRoundTableId();
    }

    private List<JudgeAccount> resolveCaptains(List<String> publicIds, Integer tableCount) {
        List<String> normalized = safeList(publicIds).stream().filter(StringUtils::hasText).toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (normalized.size() > tableCount) {
            throw new BaseException("桌长数量不能超过桌数");
        }
        return new ArrayList<>(roundQuerySupport.loadJudgeByPublicIds(new LinkedHashSet<>(normalized)).values());
    }

    private boolean isTerminalRound(CompetitionRound round) {
        List<RoundTable> tables = roundQuerySupport.listRoundTables(round.getId());
        return RoundType.RANKING.name().equals(round.getRoundType())
                && !tables.isEmpty()
                && tables.stream().allMatch(table -> RoundTargetMode.CHAMPION.name().equals(table.getTargetMode()));
    }

    private void validateBaseCaptains(List<JudgeTable> baseTables, List<JudgeAssignment> assignments) {
        Map<Long, List<JudgeAssignment>> assignmentsByTable = assignments.stream().collect(Collectors.groupingBy(JudgeAssignment::getTableId));
        for (JudgeTable table : baseTables) {
            long captainCount = assignmentsByTable.getOrDefault(table.getId(), List.of())
                    .stream()
                    .filter(assignment -> JudgeRoleType.CAPTAIN.name().equals(assignment.getRole()))
                    .count();
            if (captainCount != 1) {
                throw new BaseException(table.getTableName() + "必须有且只有 1 名桌长");
            }
        }
    }

    private int resolveCandidateTableIndex(RoundCreationStrategy strategy, int candidateIndex, int tableCount) {
        if (strategy == RoundCreationStrategy.MERGE_ONE) {
            return 0;
        }
        return candidateIndex % tableCount;
    }

    private String resolveNextRoundTableName(int roundNo, int index, int tableCount) {
        if (tableCount == 1) {
            return roundNo >= 3 ? "决赛桌" : roundNo + "A桌";
        }
        return roundNo + String.valueOf((char) ('A' + index)) + "桌";
    }

    private CompetitionType resolveCompetitionType(Competition competition) {
        return CompetitionType.of(competition == null ? null : competition.getCompetitionType());
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? List.of() : source;
    }
}
