package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundJudgeRankingDraftMapper;
import com.beercompetition.mapper.RoundTableConfirmationMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.FirstRoundCreateRequest;
import com.beercompetition.pojo.dto.AdminConfirmationOverrideRequest;
import com.beercompetition.pojo.dto.NextRoundCreateRequest;
import com.beercompetition.pojo.dto.RankingDraftSaveRequest;
import com.beercompetition.pojo.dto.RankingResultItemRequest;
import com.beercompetition.pojo.dto.RankingSubmitRequest;
import com.beercompetition.pojo.dto.RoundAllocationRequest;
import com.beercompetition.pojo.dto.RoundTableAllocationRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.JudgeTaskType;
import com.beercompetition.pojo.enums.RoundCreationStrategy;
import com.beercompetition.pojo.enums.RoundEntryStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundJudgeRankingDraft;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableConfirmation;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.pojo.vo.JudgeRoundTableVO;
import com.beercompetition.pojo.vo.JudgeTaskVO;
import com.beercompetition.pojo.vo.ResultDraftVO;
import com.beercompetition.pojo.vo.RoundRankingSlotVO;
import com.beercompetition.pojo.vo.ScoreConfirmationEntryVO;
import com.beercompetition.pojo.vo.ScoreConfirmationVO;
import com.beercompetition.pojo.vo.RoundTableMemberVO;
import com.beercompetition.pojo.vo.RoundTableJudgeEntryScoreVO;
import com.beercompetition.pojo.vo.RoundTableJudgeProgressVO;
import com.beercompetition.pojo.vo.RoundTableVO;
import com.beercompetition.service.AwardService;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.RoundService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoundServiceImpl implements RoundService {

    private static final int FLAG_TRUE = 1;
    private static final int FLAG_FALSE = 0;
    private static final int FULL_PROGRESS = 100;
    private static final String CATEGORY_MODE_EMPTY = "EMPTY";
    private static final String CATEGORY_MODE_MIXED = "MIXED";
    private static final String CATEGORY_MODE_CATEGORY = "CATEGORY";
    private static final String SLOT_GOLD = "金奖";
    private static final String SLOT_SILVER = "银奖";
    private static final String SLOT_BRONZE = "铜奖";

    private final CompetitionMapper competitionMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;
    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;
    private final BreweryMapper breweryMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final EntryDeliveryMapper entryDeliveryMapper;
    private final EntryRefundMapper entryRefundMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundResultMapper roundResultMapper;
    private final RoundTableConfirmationMapper roundTableConfirmationMapper;
    private final RoundJudgeRankingDraftMapper roundJudgeRankingDraftMapper;
    private final EntryScanLabelService entryScanLabelService;
    private final AwardService awardService;
    private final ObjectMapper objectMapper;

    @Override
    public List<CompetitionRoundVO> listCompetitionRounds(Long competitionId) {
        requireCompetition(competitionId);
        List<CompetitionRound> rounds = listRounds(competitionId);
        if (rounds.isEmpty()) {
            return List.of();
        }
        List<RoundTable> tables = listRoundTablesByCompetition(competitionId);
        List<RoundTableEntry> tableEntries = listRoundEntriesByCompetition(competitionId);
        List<RoundResult> results = listResultsByCompetition(competitionId);
        Map<Long, List<RoundTable>> tablesByRound = tables.stream().collect(Collectors.groupingBy(RoundTable::getRoundId));
        Map<Long, List<RoundTableEntry>> entriesByTable = tableEntries.stream().collect(Collectors.groupingBy(RoundTableEntry::getRoundTableId));
        Map<Long, List<RoundResult>> resultsByTable = results.stream().collect(Collectors.groupingBy(RoundResult::getRoundTableId));
        Map<Long, BeerEntry> entryById = loadEntries(tableEntries.stream().map(RoundTableEntry::getBeerEntryId).collect(Collectors.toSet()));
        Map<Long, String> categoryNameById = listCategoryNames(competitionId);
        List<RoundTableMember> members = listMembers(tables.stream().map(RoundTable::getId).toList());
        Set<Long> judgeIds = new HashSet<>();
        tables.stream().map(RoundTable::getCaptainJudgeId).filter(Objects::nonNull).forEach(judgeIds::add);
        members.stream().map(RoundTableMember::getJudgeAccountId).filter(Objects::nonNull).forEach(judgeIds::add);
        Map<Long, JudgeAccount> judgeById = loadJudges(judgeIds);
        Map<Long, List<RoundTableMember>> membersByTable = members.stream().collect(Collectors.groupingBy(RoundTableMember::getRoundTableId));

        return rounds.stream()
                .map(round -> toRoundVO(round, tablesByRound.getOrDefault(round.getId(), List.of()), entriesByTable,
                        resultsByTable, entryById, judgeById, membersByTable, categoryNameById))
                .toList();
    }

    @Override
    public List<CompetitionEntryVO> listEntryPool(Long competitionId) {
        requireCompetition(competitionId);
        Map<Long, String> categoryNameById = listCategoryNames(competitionId);
        Map<String, CompetitionStyleConfig> styleByName = listStyleSnapshot(competitionId);
        Map<Long, RoundResult> latestResultByEntry = latestResultByEntry(competitionId);
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .orderByDesc(BeerEntry::getCreateTime)
                .orderByAsc(BeerEntry::getId));
        Map<Long, RoundTable> tableById = loadRoundTables(latestResultByEntry.values().stream()
                .map(RoundResult::getRoundTableId)
                .collect(Collectors.toSet()));
        Map<Long, Brewery> breweryById = loadBreweries(entries.stream().map(BeerEntry::getBreweryId).collect(Collectors.toSet()));
        Map<Long, EntryPayment> paymentByEntryId = loadPayments(entries.stream().map(BeerEntry::getId).collect(Collectors.toSet()));
        Map<Long, EntryDelivery> deliveryByEntryId = loadDeliveries(entries.stream().map(BeerEntry::getId).collect(Collectors.toSet()));
        Map<Long, EntryRefund> refundByEntryId = loadLatestRefunds(entries.stream().map(BeerEntry::getId).collect(Collectors.toSet()));
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entries.stream().map(BeerEntry::getId).toList());
        return entries.stream()
                .map(entry -> toEntryVO(entry, categoryNameById, styleByName, latestResultByEntry.get(entry.getId()), tableById,
                        breweryById.get(entry.getBreweryId()), paymentByEntryId.get(entry.getId()), deliveryByEntryId.get(entry.getId()),
                        refundByEntryId.get(entry.getId()), labelByEntryId.get(entry.getId())))
                .toList();
    }

    @Override
    public List<ResultDraftVO> buildResultDrafts(Long competitionId) {
        requireCompetition(competitionId);
        List<CompetitionRound> rounds = listRounds(competitionId);
        if (rounds.isEmpty()) {
            return List.of();
        }
        CompetitionRound lastLocked = rounds.stream()
                .filter(round -> RoundStatus.LOCKED.name().equals(round.getStatus()))
                .max(Comparator.comparing(CompetitionRound::getRoundNo))
                .orElse(null);
        if (lastLocked == null) {
            return List.of();
        }
        Map<Long, BeerEntry> entryById = loadEntries(roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                        .eq(RoundResult::getCompetitionId, competitionId)
                        .eq(RoundResult::getRoundId, lastLocked.getId()))
                .stream()
                .map(RoundResult::getBeerEntryId)
                .collect(Collectors.toSet()));
        Map<Long, String> categoryNameById = listCategoryNames(competitionId);
        return roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                        .eq(RoundResult::getCompetitionId, competitionId)
                        .eq(RoundResult::getRoundId, lastLocked.getId())
                        .in(RoundResult::getResultType, RoundResultType.RANK.name(), RoundResultType.AWARD_CANDIDATE.name(), RoundResultType.CHAMPION.name())
                        .orderByAsc(RoundResult::getRankNo)
                        .orderByAsc(RoundResult::getId))
                .stream()
                .map(result -> {
                    BeerEntry entry = entryById.get(result.getBeerEntryId());
                    return ResultDraftVO.builder()
                            .category(entry == null ? "-" : categoryNameById.getOrDefault(entry.getCategoryId(), "-"))
                            .slot(resolveSlotLabel(result))
                            .uuid(entry == null ? null : entry.getUuid())
                            .beerEntryId(result.getBeerEntryId())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFirstRound(Long competitionId, FirstRoundCreateRequest request) {
        // 1) 查询比赛、基础桌和可分配酒款
        Competition competition = requireCompetition(competitionId);
        if (!isPreJudgingStage(parseCompetitionStatus(competition))) {
            throw new BaseException("评审已开始，不能再创建第一轮编排");
        }
        if (competitionRoundMapper.selectCount(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .eq(CompetitionRound::getRoundNo, 1)) > 0) {
            throw new BaseException("第一轮已经创建");
        }
        List<JudgeTable> baseTables = listBaseTables(competitionId);
        if (baseTables.isEmpty()) {
            throw new BaseException("请先配置基础评审桌");
        }
        List<JudgeAssignment> assignments = listBaseAssignments(competitionId);
        validateBaseCaptains(baseTables, assignments);
        List<BeerEntry> entries = listStoredEntries(competitionId);
        if (entries.isEmpty()) {
            throw new BaseException("没有可分配的已入库酒款");
        }

        // 2) 创建第一轮
        CompetitionRound round = CompetitionRound.builder()
                .competitionId(competitionId)
                .roundNo(1)
                .roundName("第一轮")
                .roundType(RoundType.SCORE.name())
                .status(RoundStatus.DRAFT.name())
                .sortOrder(1)
                .build();
        competitionRoundMapper.insert(round);
        if (request.getTables() != null && !request.getTables().isEmpty()) {
            RoundAllocationRequest allocation = new RoundAllocationRequest();
            allocation.setTables(request.getTables());
            validateAllocationRequest(round, allocation);
            saveAllocationTables(competitionId, round, allocation);
            return;
        }

        // 3) 没有传入酒款草稿时，只按基础桌生成空的第一轮桌
        Map<Long, List<JudgeAssignment>> assignmentsByTable = assignments.stream().collect(Collectors.groupingBy(JudgeAssignment::getTableId));
        List<RoundTable> roundTables = new ArrayList<>();
        for (int index = 0; index < baseTables.size(); index++) {
            JudgeTable baseTable = baseTables.get(index);
            Long captainId = findCaptainId(assignmentsByTable.getOrDefault(baseTable.getId(), List.of()));
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
        Competition competition = requireCompetition(competitionId);
        CompetitionRound round = requireRound(competitionId, roundId);
        validateCompetitionStageForRoundAllocation(competition, round);
        if (!RoundStatus.DRAFT.name().equals(round.getStatus())) {
            throw new BaseException("只有草稿轮次可以保存编排");
        }
        validateAllocationRequest(round, request);

        // 2) 清理旧编排数据
        List<RoundTable> oldTables = listRoundTables(roundId);
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
        Map<String, JudgeAccount> captainMap = loadJudgeByPublicIds(request.getTables().stream()
                .map(RoundTableAllocationRequest::getCaptainPublicId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));
        Map<String, JudgeAccount> participantMap = loadJudgeByPublicIds(request.getTables().stream()
                .flatMap(table -> safeList(table.getParticipantPublicIds()).stream())
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));
        Map<String, BeerEntry> entryMap = loadEntryByUuids(competitionId, request.getTables().stream()
                .flatMap(table -> safeList(table.getEntryUuids()).stream())
                .collect(Collectors.toSet()));
        validateEntrySource(round, entryMap.values().stream().toList());
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
                    .targetMode(resolveTargetMode(round, item.getTargetMode()))
                    .status(RoundStatus.DRAFT.name())
                    .sortOrder(item.getSortOrder() == null ? tableIndex : item.getSortOrder())
                    .build();
            roundTableMapper.insert(table);
            if (captain != null) {
                insertCaptainMember(table, captain.getId());
            }
            if (RoundType.SCORE.name().equals(round.getRoundType())) {
                insertScoreRoundMembersFromBaseTable(table, item.getName().trim());
            } else {
                insertRankingParticipants(table, item.getParticipantPublicIds(), participantMap, captain == null ? null : captain.getId());
            }
            int entryIndex = 0;
            for (String uuid : safeList(item.getEntryUuids())) {
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
    public void syncRoundCandidates(Long competitionId, Long roundId) {
        // 1) 查询轮次与来源轮次
        CompetitionRound round = requireRound(competitionId, roundId);
        if (!RoundType.RANKING.name().equals(round.getRoundType())) {
            throw new BaseException("只有后续排序轮需要更新晋级酒款");
        }
        if (!RoundStatus.DRAFT.name().equals(round.getStatus())) {
            throw new BaseException("只有草稿轮次可以更新晋级酒款");
        }
        CompetitionRound sourceRound = requireRound(competitionId, round.getSourceRoundId());
        if (!RoundStatus.LOCKED.name().equals(sourceRound.getStatus())) {
            throw new BaseException("请先锁定上一轮，再更新晋级酒款");
        }
        validateSourceIsLatestLockedRound(competitionId, sourceRound);

        // 2) 计算候选酒款并保留已有分配
        List<RoundTable> tables = listRoundTables(roundId);
        if (tables.isEmpty()) {
            throw new BaseException("当前轮次至少需要 1 张桌");
        }
        List<RoundResult> candidates = filterCandidatesForTargetMode(listCandidateResults(sourceRound.getId()), tables.get(0).getTargetMode());
        if (candidates.isEmpty()) {
            throw new BaseException("上一轮没有可同步的候选酒款");
        }
        Set<Long> candidateEntryIds = candidates.stream()
                .map(RoundResult::getBeerEntryId)
                .collect(Collectors.toSet());
        List<RoundTableEntry> existingEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, roundId));
        List<Long> staleEntryIds = existingEntries.stream()
                .filter(entry -> !candidateEntryIds.contains(entry.getBeerEntryId()))
                .map(RoundTableEntry::getId)
                .toList();
        if (!staleEntryIds.isEmpty()) {
            roundTableEntryMapper.deleteBatchIds(staleEntryIds);
        }
        Set<Long> existingEntryIds = existingEntries.stream()
                .filter(entry -> candidateEntryIds.contains(entry.getBeerEntryId()))
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        Map<Long, Integer> nextSortOrderByTable = new HashMap<>();
        for (RoundTable table : tables) {
            int maxSort = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                            .eq(RoundTableEntry::getRoundTableId, table.getId()))
                    .stream()
                    .map(RoundTableEntry::getSortOrder)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(-1);
            nextSortOrderByTable.put(table.getId(), maxSort + 1);
        }

        // 3) 将缺失候选按桌次轮转补入草稿
        int index = 0;
        for (RoundResult source : candidates) {
            if (existingEntryIds.contains(source.getBeerEntryId())) {
                continue;
            }
            RoundTable table = tables.get(resolveCandidateTableIndex(RoundCreationStrategy.EVEN_SPLIT, index++, tables.size()));
            roundTableEntryMapper.insert(RoundTableEntry.builder()
                    .competitionId(competitionId)
                    .roundId(roundId)
                    .roundTableId(table.getId())
                    .beerEntryId(source.getBeerEntryId())
                    .sourceRoundTableId(source.getRoundTableId())
                    .status(RoundEntryStatus.ASSIGNED.name())
                    .sortOrder(nextSortOrderByTable.compute(table.getId(), (key, value) -> value == null ? 1 : value + 1) - 1)
                    .build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraftRound(Long competitionId, Long roundId) {
        // 1) 查询轮次并限制只能删除草稿排序轮
        requireCompetition(competitionId);
        CompetitionRound round = requireRound(competitionId, roundId);
        if (!RoundType.RANKING.name().equals(round.getRoundType())) {
            throw new BaseException("只能删除排序轮草稿");
        }
        if (!RoundStatus.DRAFT.name().equals(round.getStatus())) {
            throw new BaseException("只有草稿轮次可以删除");
        }

        // 2) 清理草稿轮关联数据
        List<Long> tableIds = listRoundTables(roundId).stream()
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
    public void publishRound(Long competitionId, Long roundId) {
        // 1) 查询轮次并校验发布条件
        Competition competition = requireCompetition(competitionId);
        CompetitionRound round = requireRound(competitionId, roundId);
        if (!RoundStatus.DRAFT.name().equals(round.getStatus())) {
            throw new BaseException("只有草稿轮次可以发布");
        }
        validateCompetitionStageForRoundPublish(competition, round);
        validateRoundReady(round);

        // 2) 更新轮次和桌任务状态
        String nextStatus = RoundType.SCORE.name().equals(round.getRoundType()) ? RoundStatus.PUBLISHED.name() : RoundStatus.IN_PROGRESS.name();
        round.setStatus(nextStatus);
        round.setPublishedTime(LocalDateTime.now());
        competitionRoundMapper.updateById(round);
        for (RoundTable table : listRoundTables(roundId)) {
            table.setStatus(nextStatus);
            roundTableMapper.updateById(table);
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            competition.setStatus(CompetitionStatus.JUDGING.name());
            competitionMapper.updateById(competition);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeFirstRound(Long competitionId, Long roundId) {
        // 1) 查询第一轮并校验评分结果
        Competition competition = requireCompetition(competitionId);
        if (parseCompetitionStatus(competition) != CompetitionStatus.JUDGING) {
            throw new BaseException("只有评审中的比赛可以确认第一轮完成");
        }
        CompetitionRound round = requireRound(competitionId, roundId);
        if (!RoundType.SCORE.name().equals(round.getRoundType())) {
            throw new BaseException("只有第一轮评分制轮次可以执行此操作");
        }
        List<RoundTableEntry> entries = listRoundEntries(roundId);
        if (entries.isEmpty()) {
            throw new BaseException("第一轮没有酒款分配");
        }
        List<RoundTable> tables = listRoundTables(roundId);
        Map<Long, RoundTable> tableById = tables.stream().collect(Collectors.toMap(RoundTable::getId, Function.identity()));
        Map<Long, ScoreRecord> finalScoreByEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, competitionId)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        Map<Long, List<RoundTableEntry>> entriesByTable = entries.stream().collect(Collectors.groupingBy(RoundTableEntry::getRoundTableId));
        for (RoundTable table : tables) {
            List<RoundTableEntry> tableEntries = entriesByTable.getOrDefault(table.getId(), List.of());
            List<Long> missing = tableEntries.stream()
                    .map(RoundTableEntry::getBeerEntryId)
                    .filter(entryId -> !finalScoreByEntry.containsKey(entryId))
                    .toList();
            if (!missing.isEmpty()) {
                throw new BaseException(table.getTableName() + "还有酒款未完成桌长汇总");
            }
            long advancedCount = tableEntries.stream()
                    .map(RoundTableEntry::getBeerEntryId)
                    .map(finalScoreByEntry::get)
                    .filter(Objects::nonNull)
                    .filter(score -> Objects.equals(score.getAdvancedFlag(), FLAG_TRUE))
                    .count();
            if (advancedCount != table.getTargetCount()) {
                throw new BaseException(table.getTableName() + "晋级数量必须等于目标数量 " + table.getTargetCount());
            }
        }

        // 2) 根据桌长汇总写入晋级结果
        roundResultMapper.delete(new LambdaQueryWrapper<RoundResult>().eq(RoundResult::getRoundId, roundId));
        for (RoundTableEntry entry : entries) {
            ScoreRecord finalScore = finalScoreByEntry.get(entry.getBeerEntryId());
            boolean advanced = Objects.equals(finalScore.getAdvancedFlag(), FLAG_TRUE);
            RoundTable table = tableById.get(entry.getRoundTableId());
            entry.setStatus(advanced ? RoundEntryStatus.ADVANCED.name() : RoundEntryStatus.ELIMINATED.name());
            roundTableEntryMapper.updateById(entry);
            if (advanced) {
                roundResultMapper.insert(RoundResult.builder()
                        .competitionId(competitionId)
                        .roundId(roundId)
                        .roundTableId(entry.getRoundTableId())
                        .beerEntryId(entry.getBeerEntryId())
                        .resultType(RoundResultType.ADVANCE.name())
                        .rankNo(null)
                        .slotLabel(table == null ? "晋级" : table.getTableName() + "晋级")
                        .submittedBy(finalScore.getJudgeAccountId())
                        .submittedTime(LocalDateTime.now())
                        .lockedFlag(FLAG_TRUE)
                        .build());
            }
        }

        // 3) 锁定第一轮
        lockRoundAndTables(round);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNextRound(Long competitionId, NextRoundCreateRequest request) {
        // 1) 查询来源轮次和晋级候选
        Competition competition = requireCompetition(competitionId);
        CompetitionStatus competitionStatus = parseCompetitionStatus(competition);
        RoundTargetMode targetMode = RoundTargetMode.of(request.getTargetMode());
        boolean creatingChampionRound = targetMode == RoundTargetMode.CHAMPION && competitionStatus == CompetitionStatus.RESULT_CONFIRMING;
        if (competitionStatus != CompetitionStatus.JUDGING && !creatingChampionRound) {
            throw new BaseException("只有评审中的比赛可以创建后续轮次");
        }
        validateTargetCountForMode(request.getRoundName(), targetMode.name(), request.getTargetCount());
        CompetitionRound sourceRound = requireRound(competitionId, request.getSourceRoundId());
        if (isTerminalRound(sourceRound)) {
            throw new BaseException("决赛轮已是最后一轮，不能继续创建轮次");
        }
        boolean sourceLocked = RoundStatus.LOCKED.name().equals(sourceRound.getStatus());
        if (sourceLocked) {
            validateSourceIsLatestLockedRound(competitionId, sourceRound);
        }
        if (competitionRoundMapper.selectCount(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .eq(CompetitionRound::getSourceRoundId, sourceRound.getId())) > 0) {
            throw new BaseException("已基于该轮次创建过下一轮");
        }
        List<RoundResult> candidates = sourceLocked
                ? filterCandidatesForTargetMode(listCandidateResults(sourceRound.getId()), targetMode.name())
                : List.of();
        if (sourceLocked && candidates.isEmpty()) {
            throw new BaseException("上一轮没有可用于创建下一轮的候选酒款");
        }
        int nextRoundNo = listRounds(competitionId).stream().mapToInt(CompetitionRound::getRoundNo).max().orElse(1) + 1;
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockRound(Long competitionId, Long roundId) {
        // 1) 查询轮次并校验锁定状态
        Competition competition = requireCompetition(competitionId);
        if (!isRankingRoundStage(parseCompetitionStatus(competition))) {
            throw new BaseException("当前阶段不能锁定排序轮");
        }
        CompetitionRound round = requireRound(competitionId, roundId);
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            completeFirstRound(competitionId, roundId);
            return;
        }
        if (!RoundStatus.SUBMITTED.name().equals(round.getStatus())) {
            throw new BaseException("只有已提交排序的轮次可以锁定");
        }

        // 2) 锁定排序结果
        roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>().eq(RoundResult::getRoundId, roundId))
                .forEach(result -> {
                    result.setLockedFlag(FLAG_TRUE);
                    roundResultMapper.updateById(result);
                });

        // 3) 锁定轮次和桌任务
        lockRoundAndTables(round);
        if (isAwardRound(round)) {
            awardService.generateAwardDraftsForRound(competitionId, roundId);
            competition.setStatus(CompetitionStatus.RESULT_CONFIRMING.name());
            competitionMapper.updateById(competition);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishResults(Long competitionId) {
        // 1) 校验比赛和正式奖项
        Competition competition = requireCompetition(competitionId);
        if (parseCompetitionStatus(competition) != CompetitionStatus.RESULT_CONFIRMING) {
            throw new BaseException("请先完成评审并确认奖项，再发布结果");
        }
        CompetitionRound lastLocked = findLastLockedRound(competitionId);
        if (lastLocked == null || !isTerminalRound(lastLocked)) {
            throw new BaseException("决赛轮结果未锁定，暂不能发布结果");
        }
        awardService.publishAwards(competitionId);

        // 2) 发布比赛状态
        competition.setStatus(CompetitionStatus.PUBLISHED.name());
        competitionMapper.updateById(competition);
    }

    @Override
    public List<JudgeTaskVO> listMyTasks() {
        Long judgeId = BaseContext.getCurrentId();
        JudgeAccount judge = requireActiveJudge(judgeId);
        List<RoundTableMember> members = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getJudgeAccountId, judgeId));
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, RoundTable> tableById = loadRoundTables(members.stream().map(RoundTableMember::getRoundTableId).collect(Collectors.toSet()));
        Map<Long, CompetitionRound> roundById = loadRounds(tableById.values().stream().map(RoundTable::getRoundId).collect(Collectors.toSet()));
        Map<Long, Competition> competitionById = loadCompetitions(roundById.values().stream().map(CompetitionRound::getCompetitionId).collect(Collectors.toSet()));
        return members.stream()
                .map(member -> buildJudgeTask(judge, member, tableById.get(member.getRoundTableId()), roundById, competitionById))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public JudgeRoundTableVO getMyRoundTable(Long roundTableId) {
        Long judgeId = BaseContext.getCurrentId();
        requireActiveJudge(judgeId);
        RoundTable table = requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null) {
            throw new ResourceNotFoundException("轮次不存在");
        }
        if (RoundType.SCORE.name().equals(round.getRoundType()) && !isScoreRoundVisibleStatus(round.getStatus())) {
            throw new BaseException("当前评分轮次未发布或已锁定");
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && !isRankingVisibleStatus(round.getStatus())) {
            throw new BaseException("当前排序轮次未发布或已锁定");
        }
        RoundTableMember member = requireRoundTableMember(roundTableId, judgeId);
        Map<Long, String> categoryNameById = listCategoryNames(table.getCompetitionId());
        Map<String, CompetitionStyleConfig> styleByName = listStyleSnapshot(table.getCompetitionId());
        List<RoundTableEntry> entries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, roundTableId)
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
        Map<Long, BeerEntry> entryById = loadEntries(entries.stream().map(RoundTableEntry::getBeerEntryId).collect(Collectors.toSet()));
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entryById.keySet());
        List<RoundResult> results = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundTableId, roundTableId)
                .orderByAsc(RoundResult::getRankNo));
        return JudgeRoundTableVO.builder()
                .roundTableId(roundTableId)
                .roundId(round.getId())
                .roundName(round.getRoundName())
                .roundType(round.getRoundType())
                .tableName(table.getTableName())
                .targetCount(table.getTargetCount())
                .expectedJudgeCount(resolveExpectedJudgeCount(table.getId()))
                .targetMode(table.getTargetMode())
                .status(table.getStatus())
                .canSubmitTableScore(canSubmitScoreRoundTable(member, round, table))
                .canSubmitRanking(canSubmitRanking(member, round))
                .scoreConfirmation(RoundType.SCORE.name().equals(round.getRoundType()) ? buildScoreConfirmation(table, member) : null)
                .entries(entries.stream()
                        .map(item -> toEntryVO(
                                entryById.get(item.getBeerEntryId()),
                                categoryNameById,
                                styleByName,
                                null,
                                Map.of(),
                                null,
                                null,
                                null,
                                null,
                                labelByEntryId.get(item.getBeerEntryId())))
                        .toList())
                .rankings(buildRankings(table, results, entryById))
                .myRankingDraft(RoundType.RANKING.name().equals(round.getRoundType()) ? buildMyRankingDraft(table, entryById, judgeId) : List.of())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitScoreRoundTable(Long roundTableId) {
        // 1) 查询评分桌并校验桌长权限
        Long judgeId = BaseContext.getCurrentId();
        requireActiveJudge(judgeId);
        RoundTable table = requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null) {
            throw new ResourceNotFoundException("轮次不存在");
        }
        if (!RoundType.SCORE.name().equals(round.getRoundType()) || !RoundStatus.PUBLISHED.name().equals(round.getStatus())) {
            throw new BaseException("当前轮次不能提交本桌结果");
        }
        if (!RoundStatus.PUBLISHED.name().equals(table.getStatus())) {
            throw new BaseException("本桌结果已提交或已锁定");
        }
        requireRankingCaptainMember(roundTableId, judgeId);
        validateScoreRoundTableReady(table);
        validateScoreRoundTableConfirmations(table);

        // 2) 提交本桌汇总；全部桌提交后推进轮次到待管理员确认
        table.setStatus(RoundStatus.SUBMITTED.name());
        roundTableMapper.updateById(table);
        if (listRoundTables(round.getId()).stream().allMatch(item -> RoundStatus.SUBMITTED.name().equals(item.getStatus()))) {
            round.setStatus(RoundStatus.SUBMITTED.name());
            round.setSubmittedTime(LocalDateTime.now());
            competitionRoundMapper.updateById(round);
        }
    }

    @Override
    public ScoreConfirmationVO getScoreConfirmation(Long roundTableId) {
        Long judgeId = BaseContext.getCurrentId();
        requireActiveJudge(judgeId);
        RoundTable table = requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !RoundType.SCORE.name().equals(round.getRoundType()) || !isScoreRoundVisibleStatus(round.getStatus())) {
            throw new BaseException("当前评分轮次不可查看");
        }
        RoundTableMember member = requireRoundTableMember(roundTableId, judgeId);
        return buildScoreConfirmation(table, member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScoreConfirmationVO confirmScoreRoundTable(Long roundTableId) {
        Long judgeId = BaseContext.getCurrentId();
        requireActiveJudge(judgeId);
        RoundTable table = requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !RoundType.SCORE.name().equals(round.getRoundType()) || !RoundStatus.PUBLISHED.name().equals(round.getStatus())) {
            throw new BaseException("当前轮次不能确认本桌结果");
        }
        if (!RoundStatus.PUBLISHED.name().equals(table.getStatus())) {
            throw new BaseException("本桌结果已提交或已锁定");
        }
        RoundTableMember member = requireRoundTableMember(roundTableId, judgeId);
        if (JudgeRoleType.CAPTAIN.name().equals(member.getRole()) || !Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE)) {
            throw new ForbiddenException("当前账号不需要确认本桌结果");
        }
        validateScoreRoundTableReady(table);
        int version = currentResultVersion(table);
        RoundTableConfirmation existing = roundTableConfirmationMapper.selectOne(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, roundTableId)
                .eq(RoundTableConfirmation::getJudgeAccountId, judgeId)
                .eq(RoundTableConfirmation::getResultVersion, version));
        if (existing == null) {
            roundTableConfirmationMapper.insert(RoundTableConfirmation.builder()
                    .roundTableId(roundTableId)
                    .judgeAccountId(judgeId)
                    .resultVersion(version)
                    .status("AGREED")
                    .confirmedTime(LocalDateTime.now())
                    .build());
        }
        return buildScoreConfirmation(roundTableMapper.selectById(roundTableId), member);
    }

    private Integer resolveExpectedJudgeCount(Long roundTableId) {
        return Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, roundTableId)
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRanking(Long roundTableId, RankingSubmitRequest request) {
        // 1) 查询排序桌并校验桌长权限
        Long judgeId = BaseContext.getCurrentId();
        requireActiveJudge(judgeId);
        RoundTable table = requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (!RoundType.RANKING.name().equals(round.getRoundType()) || !isRankingEditableStatus(round.getStatus())) {
            throw new BaseException("当前轮次不能提交排序");
        }
        if (RoundStatus.LOCKED.name().equals(table.getStatus())) {
            throw new BaseException("本桌排序已锁定，不能调整");
        }
        requireRankingCaptainMember(roundTableId, judgeId);
        validateRankingSubmit(table, request);

        // 2) 写入排序结果和桌内酒款状态
        roundResultMapper.delete(new LambdaQueryWrapper<RoundResult>().eq(RoundResult::getRoundTableId, roundTableId));
        Set<Long> rankedEntryIds = request.getResults().stream().map(RankingResultItemRequest::getBeerEntryId).collect(Collectors.toSet());
        for (RankingResultItemRequest item : request.getResults()) {
            int rank = item.getRankNo();
            roundResultMapper.insert(RoundResult.builder()
                    .competitionId(table.getCompetitionId())
                    .roundId(table.getRoundId())
                    .roundTableId(roundTableId)
                    .beerEntryId(item.getBeerEntryId())
                    .resultType(RoundResultType.RANK.name())
                    .rankNo(rank)
                    .slotLabel(StringUtils.hasText(item.getSlotLabel()) ? item.getSlotLabel().trim() : defaultSlotLabel(table.getTargetMode(), rank))
                    .submittedBy(judgeId)
                    .submittedTime(LocalDateTime.now())
                    .lockedFlag(FLAG_FALSE)
                    .build());
        }
        roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>().eq(RoundTableEntry::getRoundTableId, roundTableId))
                .forEach(entry -> {
                    entry.setStatus(rankedEntryIds.contains(entry.getBeerEntryId()) ? RoundEntryStatus.RANKED.name() : RoundEntryStatus.ELIMINATED.name());
                    roundTableEntryMapper.updateById(entry);
                });

        // 3) 更新桌和轮次提交状态
        table.setStatus(RoundStatus.SUBMITTED.name());
        roundTableMapper.updateById(table);
        if (listRoundTables(round.getId()).stream().allMatch(item -> RoundStatus.SUBMITTED.name().equals(item.getStatus()))) {
            round.setStatus(RoundStatus.SUBMITTED.name());
            round.setSubmittedTime(LocalDateTime.now());
            competitionRoundMapper.updateById(round);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRankingDraft(Long roundTableId, RankingDraftSaveRequest request) {
        Long judgeId = BaseContext.getCurrentId();
        requireActiveJudge(judgeId);
        RoundTable table = requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !RoundType.RANKING.name().equals(round.getRoundType()) || !isRankingEditableStatus(round.getStatus())) {
            throw new BaseException("当前轮次不能保存参考排序");
        }
        if (RoundStatus.LOCKED.name().equals(table.getStatus())) {
            throw new BaseException("本桌排序已锁定，不能调整参考排序");
        }
        requireRoundTableMember(roundTableId, judgeId);
        validateRankingDraft(table, request);
        roundJudgeRankingDraftMapper.delete(new LambdaQueryWrapper<RoundJudgeRankingDraft>()
                .eq(RoundJudgeRankingDraft::getRoundTableId, roundTableId)
                .eq(RoundJudgeRankingDraft::getJudgeAccountId, judgeId));
        roundJudgeRankingDraftMapper.insert(RoundJudgeRankingDraft.builder()
                .competitionId(table.getCompetitionId())
                .roundId(table.getRoundId())
                .roundTableId(roundTableId)
                .judgeAccountId(judgeId)
                .rankingsJson(writeRankingDraft(request.getResults()))
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void overrideScoreConfirmation(Long competitionId, Long roundTableId, AdminConfirmationOverrideRequest request) {
        requireCompetition(competitionId);
        RoundTable table = requireRoundTable(roundTableId);
        if (!Objects.equals(table.getCompetitionId(), competitionId)) {
            throw new ResourceNotFoundException("评审桌不存在");
        }
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !RoundType.SCORE.name().equals(round.getRoundType()) || !RoundStatus.PUBLISHED.name().equals(round.getStatus())) {
            throw new BaseException("当前轮次不能现场确认");
        }
        if (!RoundStatus.PUBLISHED.name().equals(table.getStatus())) {
            throw new BaseException("本桌结果已提交或已锁定");
        }
        validateScoreRoundTableReady(table);
        table.setConfirmationOverrideFlag(FLAG_TRUE);
        table.setConfirmationOverrideReason(request.getReason().trim());
        table.setConfirmationOverrideBy(BaseContext.getCurrentId());
        table.setConfirmationOverrideTime(LocalDateTime.now());
        roundTableMapper.updateById(table);
    }

    private CompetitionRoundVO toRoundVO(CompetitionRound round,
                                         List<RoundTable> tables,
                                         Map<Long, List<RoundTableEntry>> entriesByTable,
                                         Map<Long, List<RoundResult>> resultsByTable,
                                         Map<Long, BeerEntry> entryById,
                                         Map<Long, JudgeAccount> judgeById,
                                         Map<Long, List<RoundTableMember>> membersByTable,
                                         Map<Long, String> categoryNameById) {
        Set<String> sourceEntryUuids = new LinkedHashSet<>();
        List<RoundResult> sourceCandidates = round.getSourceRoundId() == null ? List.of() : resolveCandidateResultsForRound(round, tables);
        if (!sourceCandidates.isEmpty()) {
            sourceCandidates.forEach(result -> {
                BeerEntry entry = entryById.get(result.getBeerEntryId());
                if (entry == null) {
                    entry = beerEntryMapper.selectById(result.getBeerEntryId());
                }
                if (entry != null) {
                    sourceEntryUuids.add(entry.getUuid());
                }
            });
        }
        CompetitionRound sourceRound = round.getSourceRoundId() == null ? null : competitionRoundMapper.selectById(round.getSourceRoundId());
        boolean sourceLocked = sourceRound != null && RoundStatus.LOCKED.name().equals(sourceRound.getStatus());
        Set<Long> sourceCandidateEntryIds = sourceCandidates.stream()
                .map(RoundResult::getBeerEntryId)
                .collect(Collectors.toSet());
        Set<Long> assignedEntryIds = tables.stream()
                .flatMap(table -> entriesByTable.getOrDefault(table.getId(), List.of()).stream())
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        boolean candidatesSynced = RoundType.SCORE.name().equals(round.getRoundType())
                || (sourceLocked && !sourceCandidateEntryIds.isEmpty() && assignedEntryIds.equals(sourceCandidateEntryIds));
        return CompetitionRoundVO.builder()
                .id(round.getId())
                .roundNo(round.getRoundNo())
                .name(round.getRoundName())
                .type(round.getRoundType())
                .status(round.getStatus())
                .sourceRoundId(round.getSourceRoundId())
                .sourceEntryUuids(new ArrayList<>(sourceEntryUuids))
                .sourceLocked(sourceRound == null ? null : sourceLocked)
                .candidatesSynced(candidatesSynced)
                .preparationDraft(RoundType.RANKING.name().equals(round.getRoundType())
                        && RoundStatus.DRAFT.name().equals(round.getStatus())
                        && (sourceRound == null || !sourceLocked || !candidatesSynced))
                .tables(tables.stream()
                        .sorted(Comparator.comparing(RoundTable::getSortOrder, Comparator.nullsLast(Integer::compareTo)).thenComparing(RoundTable::getId))
                        .map(table -> toRoundTableVO(table, entriesByTable.getOrDefault(table.getId(), List.of()),
                                resultsByTable.getOrDefault(table.getId(), List.of()), entryById, judgeById,
                                membersByTable.getOrDefault(table.getId(), List.of()), categoryNameById))
                        .toList())
                .build();
    }

    private RoundTableVO toRoundTableVO(RoundTable table,
                                        List<RoundTableEntry> entries,
                                        List<RoundResult> results,
                                        Map<Long, BeerEntry> entryById,
                                        Map<Long, JudgeAccount> judgeById,
                                        List<RoundTableMember> members,
                                        Map<Long, String> categoryNameById) {
        int advancedCount = (int) entries.stream()
                .filter(entry -> RoundEntryStatus.ADVANCED.name().equals(entry.getStatus()) || RoundEntryStatus.RANKED.name().equals(entry.getStatus()))
                .count();
        int finalCount = (int) scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .filter(record -> entries.stream().anyMatch(entry -> entry.getBeerEntryId().equals(record.getBeerEntryId())))
                .count();
        return RoundTableVO.builder()
                .id(table.getId())
                .name(table.getTableName())
                .captainPublicId(judgeById.get(table.getCaptainJudgeId()) == null ? "" : judgeById.get(table.getCaptainJudgeId()).getPublicId())
                .professionalCount(countMembers(members, JudgeRoleType.PROFESSIONAL.name()))
                .crossCount(countMembers(members, JudgeRoleType.CROSS.name()))
                .categoryId(table.getCategoryId())
                .categoryMode(resolveCategoryMode(table))
                .categoryName(resolveCategoryName(table, categoryNameById))
                .targetCount(table.getTargetCount())
                .targetMode(table.getTargetMode())
                .status(table.getStatus())
                .entryUuids(entries.stream()
                        .sorted(Comparator.comparing(RoundTableEntry::getSortOrder, Comparator.nullsLast(Integer::compareTo)).thenComparing(RoundTableEntry::getId))
                        .map(entry -> entryById.get(entry.getBeerEntryId()))
                        .filter(Objects::nonNull)
                        .map(BeerEntry::getUuid)
                        .toList())
                .advancedCount(advancedCount)
                .judgeProgress(resolveJudgeProgress(table, entries))
                .captainProgress(entries.isEmpty() ? 0 : finalCount * FULL_PROGRESS / entries.size())
                .resultVersion(currentResultVersion(table))
                .confirmationConfirmedCount(resolveConfirmationConfirmedCount(table))
                .confirmationRequiredCount(resolveConfirmationRequiredCount(table))
                .confirmationReady(isScoreConfirmationReady(table))
                .confirmationOverrideFlag(Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE))
                .confirmationOverrideReason(table.getConfirmationOverrideReason())
                .confirmationOverrideTime(table.getConfirmationOverrideTime())
                .members(buildRoundTableMembers(table, judgeById, members))
                .judgeDetails(buildJudgeDetails(table, entries, entryById, judgeById, members))
                .rankings(buildRankings(table, results, entryById))
                .build();
    }

    private List<RoundTableMemberVO> buildRoundTableMembers(RoundTable table,
                                                            Map<Long, JudgeAccount> judgeById,
                                                            List<RoundTableMember> members) {
        List<RoundTableMember> normalized = new ArrayList<>(members);
        if (table.getCaptainJudgeId() != null && normalized.stream().noneMatch(member -> table.getCaptainJudgeId().equals(member.getJudgeAccountId()))) {
            normalized.add(RoundTableMember.builder()
                    .roundTableId(table.getId())
                    .judgeAccountId(table.getCaptainJudgeId())
                    .role(JudgeRoleType.CAPTAIN.name())
                    .systemTaskRequired(FLAG_TRUE)
                    .build());
        }
        return normalized.stream()
                .sorted(Comparator.comparing(RoundTableMember::getRole, Comparator.nullsLast(String::compareTo))
                        .thenComparing(member -> {
                            JudgeAccount judge = judgeById.get(member.getJudgeAccountId());
                            return judge == null ? "" : judge.getName();
                        }, Comparator.nullsLast(String::compareTo)))
                .map(member -> {
                    JudgeAccount judge = judgeById.get(member.getJudgeAccountId());
                    return RoundTableMemberVO.builder()
                            .judgePublicId(judge == null ? "" : judge.getPublicId())
                            .name(judge == null ? "未知评审" : judge.getName())
                            .role(member.getRole())
                            .roleLabel(roleLabel(member))
                            .systemTaskRequired(Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE))
                            .build();
                })
                .toList();
    }

    private List<RoundTableJudgeProgressVO> buildJudgeDetails(RoundTable table,
                                                              List<RoundTableEntry> entries,
                                                              Map<Long, BeerEntry> entryById,
                                                              Map<Long, JudgeAccount> judgeById,
                                                              List<RoundTableMember> members) {
        List<RoundTableEntry> sortedEntries = entries.stream()
                .sorted(Comparator.comparing(RoundTableEntry::getSortOrder, Comparator.nullsLast(Integer::compareTo)).thenComparing(RoundTableEntry::getId))
                .toList();
        if (sortedEntries.isEmpty()) {
            return List.of();
        }
        List<RoundTableMember> taskJudges = members.stream()
                .filter(member -> Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE))
                .sorted(Comparator.comparing(RoundTableMember::getRole, Comparator.nullsLast(String::compareTo))
                        .thenComparing(member -> {
                            JudgeAccount judge = judgeById.get(member.getJudgeAccountId());
                            return judge == null ? "" : judge.getName();
                        }, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toCollection(ArrayList::new));
        if (table.getCaptainJudgeId() != null && taskJudges.stream().noneMatch(member -> table.getCaptainJudgeId().equals(member.getJudgeAccountId()))) {
            taskJudges.add(RoundTableMember.builder()
                    .roundTableId(table.getId())
                    .judgeAccountId(table.getCaptainJudgeId())
                    .role(JudgeRoleType.CAPTAIN.name())
                    .systemTaskRequired(FLAG_TRUE)
                    .build());
        }
        if (taskJudges.isEmpty()) {
            return List.of();
        }
        List<Long> entryIds = sortedEntries.stream().map(RoundTableEntry::getBeerEntryId).toList();
        List<Long> judgeIds = taskJudges.stream().map(RoundTableMember::getJudgeAccountId).toList();
        Map<String, ScoreRecord> scoreByJudgeAndEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .in(ScoreRecord::getBeerEntryId, entryIds)
                        .in(ScoreRecord::getJudgeAccountId, judgeIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_FALSE))
                .stream()
                .collect(Collectors.toMap(
                        record -> scoreKey(record.getJudgeAccountId(), record.getBeerEntryId()),
                        Function.identity(),
                        (first, second) -> first.getUpdateTime() != null
                                && (second.getUpdateTime() == null || first.getUpdateTime().isAfter(second.getUpdateTime())) ? first : second));
        return taskJudges.stream().map(member -> {
            JudgeAccount judge = judgeById.get(member.getJudgeAccountId());
            List<RoundTableJudgeEntryScoreVO> entryScores = sortedEntries.stream().map(entry -> {
                BeerEntry beerEntry = entryById.get(entry.getBeerEntryId());
                ScoreRecord score = scoreByJudgeAndEntry.get(scoreKey(member.getJudgeAccountId(), entry.getBeerEntryId()));
                return RoundTableJudgeEntryScoreVO.builder()
                        .beerUuid(beerEntry == null ? "" : beerEntry.getUuid())
                        .scored(score != null)
                        .totalScore(score == null ? null : score.getTotalScore())
                        .submittedAt(score == null ? null : score.getUpdateTime())
                        .build();
            }).toList();
            int total = entryScores.size();
            int submitted = (int) entryScores.stream().filter(score -> Boolean.TRUE.equals(score.getScored())).count();
            return RoundTableJudgeProgressVO.builder()
                    .judgePublicId(judge == null ? "" : judge.getPublicId())
                    .judgeName(judge == null ? "未知评审" : judge.getName())
                    .role(member.getRole())
                    .roleLabel(roleLabel(member))
                    .submittedCount(submitted)
                    .totalCount(total)
                    .progress(total == 0 ? 0 : submitted * FULL_PROGRESS / total)
                    .missingEntryUuids(entryScores.stream()
                            .filter(score -> !Boolean.TRUE.equals(score.getScored()))
                            .map(RoundTableJudgeEntryScoreVO::getBeerUuid)
                            .filter(StringUtils::hasText)
                            .toList())
                    .entryScores(entryScores)
                    .build();
        }).toList();
    }

    private String scoreKey(Long judgeAccountId, Long beerEntryId) {
        return judgeAccountId + ":" + beerEntryId;
    }

    private CompetitionEntryVO toEntryVO(BeerEntry entry,
                                         Map<Long, String> categoryNameById,
                                         Map<String, CompetitionStyleConfig> styleByName,
                                         RoundResult latestResult,
                                         Map<Long, RoundTable> tableById,
                                         Brewery brewery,
                                         EntryPayment payment,
                                         EntryDelivery delivery,
                                         EntryRefund refund,
                                         EntryScanLabel label) {
        if (entry == null) {
            return CompetitionEntryVO.builder().build();
        }
        CompetitionStyleConfig style = styleByName.get(entry.getStyle());
        boolean canConfirmPayment = EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus());
        boolean canMarkStored = EntryStatus.REGISTERED.name().equals(entry.getStatus());
        boolean canCancel = Set.of(EntryStatus.PENDING_PAYMENT.name(), EntryStatus.REGISTERED.name()).contains(entry.getStatus());
        return CompetitionEntryVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label == null ? null : label.getLabelCode())
                .shortCode(label == null ? null : label.getShortCode())
                .scanToken(label == null ? null : label.getScanToken())
                .name(entry.getName())
                .breweryCompanyName(brewery == null ? null : brewery.getCompanyName())
                .breweryContactName(brewery == null ? null : brewery.getContactName())
                .categoryId(entry.getCategoryId())
                .categoryName(categoryNameById.getOrDefault(entry.getCategoryId(), "-"))
                .style(entry.getStyle())
                .styleCategoryName(style == null ? null : style.getCategoryName())
                .styleCode(style == null ? null : style.getStyleCode())
                .styleDescription(style == null ? null : style.getDescription())
                .status(entry.getStatus())
                .paymentStatus(payment == null ? EntryPaymentStatus.UNPAID.name() : payment.getStatus())
                .paidTime(payment == null ? null : payment.getPaidTime())
                .refundStatus(refund == null ? null : refund.getStatus())
                .refundReason(refund == null ? null : refund.getReason())
                .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                .deliveryStatus(delivery == null ? null : delivery.getDeliveryStatus())
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .deliverySubmittedAt(delivery == null ? null : delivery.getSubmittedTime())
                .stored(Objects.equals(entry.getStoredFlag(), FLAG_TRUE))
                .advanced(latestResult != null)
                .canConfirmPayment(canConfirmPayment)
                .canMarkStored(canMarkStored)
                .canCancel(canCancel)
                .sourceTable(latestResult == null || tableById.get(latestResult.getRoundTableId()) == null ? "" : tableById.get(latestResult.getRoundTableId()).getTableName())
                .sourceResult(latestResult == null ? "" : resolveSlotLabel(latestResult))
                .build();
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

    private String resolveCategoryName(RoundTable table, Map<Long, String> categoryNameById) {
        if (table.getCategoryId() != null) {
            return categoryNameById.getOrDefault(table.getCategoryId(), "");
        }
        return CATEGORY_MODE_MIXED.equals(resolveCategoryMode(table)) ? "混合" : "";
    }

    private Map<Long, Brewery> loadBreweries(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return breweryMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Brewery::getId, Function.identity()));
    }

    private Map<Long, EntryPayment> loadPayments(Set<Long> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            return Map.of();
        }
        return entryPaymentMapper.selectList(new LambdaQueryWrapper<EntryPayment>()
                        .in(EntryPayment::getBeerEntryId, entryIds))
                .stream()
                .collect(Collectors.toMap(EntryPayment::getBeerEntryId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, EntryDelivery> loadDeliveries(Set<Long> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            return Map.of();
        }
        return entryDeliveryMapper.selectList(new LambdaQueryWrapper<EntryDelivery>()
                        .in(EntryDelivery::getBeerEntryId, entryIds))
                .stream()
                .collect(Collectors.toMap(EntryDelivery::getBeerEntryId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, EntryRefund> loadLatestRefunds(Set<Long> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, EntryRefund> refundByEntryId = new LinkedHashMap<>();
        entryRefundMapper.selectList(new LambdaQueryWrapper<EntryRefund>()
                        .in(EntryRefund::getBeerEntryId, entryIds)
                        .orderByDesc(EntryRefund::getId))
                .forEach(refund -> refundByEntryId.putIfAbsent(refund.getBeerEntryId(), refund));
        return refundByEntryId;
    }

    private Map<String, CompetitionStyleConfig> listStyleSnapshot(Long competitionId) {
        return competitionStyleConfigMapper.selectList(new LambdaQueryWrapper<CompetitionStyleConfig>()
                        .eq(CompetitionStyleConfig::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionStyleConfig::getSortOrder)
                        .orderByAsc(CompetitionStyleConfig::getId))
                .stream()
                .collect(Collectors.toMap(CompetitionStyleConfig::getName, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private void validateAllocationRequest(CompetitionRound round, RoundAllocationRequest request) {
        if (request.getTables() == null || request.getTables().isEmpty()) {
            throw new BaseException("至少需要 1 张轮次桌");
        }
        Set<String> tableNames = new HashSet<>();
        Set<String> entryUuids = new HashSet<>();
        Set<String> roundJudgePublicIds = new HashSet<>();
        for (RoundTableAllocationRequest table : request.getTables()) {
            if (!tableNames.add(table.getName().trim())) {
                throw new BaseException("轮次桌名称不能重复：" + table.getName());
            }
            if (RoundType.SCORE.name().equals(round.getRoundType()) && !StringUtils.hasText(table.getCaptainPublicId())) {
                throw new BaseException(table.getName() + "缺少桌长");
            }
            if (StringUtils.hasText(table.getCaptainPublicId()) && !roundJudgePublicIds.add(table.getCaptainPublicId())) {
                throw new BaseException("同一轮同一评审只能分配到一张桌：" + table.getCaptainPublicId());
            }
            Set<String> tableParticipantIds = new HashSet<>();
            for (String participantPublicId : safeList(table.getParticipantPublicIds())) {
                if (!StringUtils.hasText(participantPublicId)) {
                    continue;
                }
                if (participantPublicId.equals(table.getCaptainPublicId())) {
                    throw new BaseException(table.getName() + "桌长不能同时作为参与评审");
                }
                if (!tableParticipantIds.add(participantPublicId)) {
                    throw new BaseException(table.getName() + "参与评审不能重复");
                }
                if (!roundJudgePublicIds.add(participantPublicId)) {
                    throw new BaseException("同一轮同一评审只能分配到一张桌：" + participantPublicId);
                }
            }
            if (table.getTargetCount() == null || table.getTargetCount() <= 0) {
                throw new BaseException(table.getName() + "目标数量必须大于 0");
            }
            String targetMode = resolveTargetMode(round, table.getTargetMode());
            validateTargetCountForMode(table.getName(), targetMode, table.getTargetCount());
            List<String> uuids = safeList(table.getEntryUuids());
            if (!RoundTargetMode.MEDALS.name().equals(targetMode) && !uuids.isEmpty() && table.getTargetCount() > uuids.size()) {
                throw new BaseException(table.getName() + "目标数量超过酒款数");
            }
            for (String uuid : uuids) {
                if (!entryUuids.add(uuid)) {
                    throw new BaseException("同一轮同一酒款只能分配到一张桌：" + uuid);
                }
            }
        }
        if (RoundType.SCORE.name().equals(round.getRoundType()) && entryUuids.isEmpty()) {
            throw new BaseException("第一轮必须分配已入库酒款");
        }
    }

    private void validateEntrySource(CompetitionRound round, List<BeerEntry> entries) {
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            entries.stream()
                    .filter(entry -> !Objects.equals(entry.getStoredFlag(), FLAG_TRUE) || "CANCELED".equals(entry.getStatus()))
                    .findFirst()
                    .ifPresent(entry -> {
                        throw new BaseException("第一轮只能分配已入库且未取消的酒款：" + entry.getUuid());
                    });
            return;
        }
        Set<Long> allowedEntryIds = listCandidateResults(round.getSourceRoundId()).stream()
                .map(RoundResult::getBeerEntryId)
                .collect(Collectors.toSet());
        entries.stream()
                .filter(entry -> !allowedEntryIds.contains(entry.getId()))
                .findFirst()
                .ifPresent(entry -> {
                    throw new BaseException("后续轮只能分配上一轮晋级酒款：" + entry.getUuid());
                });
    }

    private void validateRoundReady(CompetitionRound round) {
        List<RoundTable> tables = listRoundTables(round.getId());
        if (tables.isEmpty()) {
            throw new BaseException("当前轮次至少需要 1 张桌");
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            validateScoreFormsReady(round.getCompetitionId());
        } else {
            CompetitionRound sourceRound = requireRound(round.getCompetitionId(), round.getSourceRoundId());
            if (!RoundStatus.LOCKED.name().equals(sourceRound.getStatus())) {
                throw new BaseException("请先锁定上一轮，再发布后续轮");
            }
            validateSourceIsLatestLockedRound(round.getCompetitionId(), sourceRound);
            validateCandidatePoolSynced(round, tables);
        }
        for (RoundTable table : tables) {
            if (table.getCaptainJudgeId() == null) {
                throw new BaseException(table.getTableName() + "缺少桌长");
            }
            List<RoundTableEntry> entries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                    .eq(RoundTableEntry::getRoundTableId, table.getId()));
            if (entries.isEmpty()) {
                throw new BaseException(table.getTableName() + "尚未分配酒款");
            }
            boolean allowEmptyMedalSlots = RoundTargetMode.MEDALS.name().equals(table.getTargetMode());
            if (table.getTargetCount() == null || table.getTargetCount() <= 0
                    || (!allowEmptyMedalSlots && table.getTargetCount() > entries.size())) {
                throw new BaseException(table.getTableName() + "目标数量不合法");
            }
            validateTargetCountForMode(table.getTableName(), table.getTargetMode(), table.getTargetCount());
            if (RoundTargetMode.MEDALS.name().equals(table.getTargetMode())
                    && (table.getCategoryId() == null || !CATEGORY_MODE_CATEGORY.equals(table.getCategoryMode()))) {
                throw new BaseException(table.getTableName() + "奖牌轮必须只包含一个投递组别");
            }
        }
    }

    private void validateCandidatePoolSynced(CompetitionRound round, List<RoundTable> tables) {
        List<RoundResult> candidates = resolveCandidateResultsForRound(round, tables);
        if (candidates.isEmpty()) {
            throw new BaseException("上一轮没有可发布的候选酒款");
        }
        Set<Long> candidateEntryIds = candidates.stream()
                .map(RoundResult::getBeerEntryId)
                .collect(Collectors.toSet());
        Set<Long> assignedEntryIds = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundId, round.getId()))
                .stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        if (!assignedEntryIds.equals(candidateEntryIds)) {
            throw new BaseException("晋级酒款已变化，请重新载入后核对分桌");
        }
    }

    private void validateTargetCountForMode(String tableName, String targetMode, Integer targetCount) {
        if (RoundTargetMode.MEDALS.name().equals(targetMode) && !Objects.equals(targetCount, 3)) {
            throw new BaseException(tableName + "奖牌排序目标必须为 3");
        }
        if (RoundTargetMode.CHAMPION.name().equals(targetMode) && !Objects.equals(targetCount, 1)) {
            throw new BaseException(tableName + "总冠军排序目标必须为 1");
        }
    }

    private void validateScoreFormsReady(Long competitionId) {
        Set<String> roles = competitionScoreConfigMapper.selectList(new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId))
                .stream()
                .map(CompetitionScoreConfig::getJudgeRoleType)
                .collect(Collectors.toSet());
        if (!roles.containsAll(Set.of(JudgeRoleType.CAPTAIN.name(), JudgeRoleType.PROFESSIONAL.name(), JudgeRoleType.CROSS.name()))) {
            throw new BaseException("第一轮发布前需要完整评分表");
        }
    }

    private void validateRankingSubmit(RoundTable table, RankingSubmitRequest request) {
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        boolean allowEmptyMedalSlots = RoundTargetMode.MEDALS.name().equals(table.getTargetMode());
        if (allowEmptyMedalSlots) {
            if (request.getResults().size() > targetCount) {
                throw new BaseException("奖项结果数量不能超过奖项槽位");
            }
        } else if (request.getResults().size() != targetCount) {
            throw new BaseException("排序结果数量必须等于目标数量");
        }
        Set<Long> tableEntryIds = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundTableId, table.getId()))
                .stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        Set<Long> resultEntryIds = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        for (RankingResultItemRequest item : request.getResults()) {
            if (item == null) {
                throw new BaseException("排序结果项不能为空");
            }
            if (!tableEntryIds.contains(item.getBeerEntryId())) {
                throw new BaseException("排序酒款不属于当前桌");
            }
            if (!resultEntryIds.add(item.getBeerEntryId())) {
                throw new BaseException("排序酒款不能重复");
            }
            if (!ranks.add(item.getRankNo())) {
                throw new BaseException("排序名次不能重复");
            }
            if (item.getRankNo() < 1 || item.getRankNo() > targetCount) {
                throw new BaseException("排序名次超出目标范围");
            }
        }
    }

    private void lockRoundAndTables(CompetitionRound round) {
        round.setStatus(RoundStatus.LOCKED.name());
        round.setLockedTime(LocalDateTime.now());
        competitionRoundMapper.updateById(round);
        for (RoundTable table : listRoundTables(round.getId())) {
            table.setStatus(RoundStatus.LOCKED.name());
            roundTableMapper.updateById(table);
        }
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
                .forEach(judge -> roundTableMemberMapper.insert(RoundTableMember.builder()
                        .roundTableId(table.getId())
                        .judgeAccountId(judge.getId())
                        .role(JudgeRoleType.PROFESSIONAL.name())
                        .systemTaskRequired(FLAG_FALSE)
                        .build()));
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

    private JudgeTaskVO buildJudgeTask(JudgeAccount judge,
                                       RoundTableMember member,
                                       RoundTable table,
                                       Map<Long, CompetitionRound> roundById,
                                       Map<Long, Competition> competitionById) {
        if (table == null) {
            return null;
        }
        CompetitionRound round = roundById.get(table.getRoundId());
        if (round == null) {
            return null;
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            if (!isScoreRoundVisibleStatus(round.getStatus())) {
                return null;
            }
            if (!RoundStatus.PUBLISHED.name().equals(table.getStatus())
                    && !JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
                return null;
            }
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && !isRankingVisibleStatus(round.getStatus())) {
            return null;
        }
        String taskType = resolveTaskType(round, member);
        if (taskType == null) {
            return null;
        }
        int totalEntries = Math.toIntExact(roundTableEntryMapper.selectCount(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, table.getId())));
        Competition competition = competitionById.get(round.getCompetitionId());
        return JudgeTaskVO.builder()
                .taskType(taskType)
                .competitionId(round.getCompetitionId())
                .competitionName(competition == null ? null : competition.getName())
                .roundId(round.getId())
                .roundName(round.getRoundName())
                .roundTableId(table.getId())
                .tableName(table.getTableName())
                .judgeRoleType(member.getRole())
                .roleLabel(roleLabel(round, member))
                .totalEntries(totalEntries)
                .completedCount(resolveCompletedCount(judge.getId(), table, taskType))
                .build();
    }

    private String resolveTaskType(CompetitionRound round, RoundTableMember member) {
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            return JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                    ? JudgeTaskType.CAPTAIN_FINALIZE.name()
                    : JudgeTaskType.SCORE_ENTRY.name();
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
            return JudgeTaskType.RANKING_ROUND.name();
        }
        if (RoundType.RANKING.name().equals(round.getRoundType())) {
            return JudgeTaskType.RANKING_PARTICIPANT.name();
        }
        return null;
    }

    private int resolveCompletedCount(Long judgeId, RoundTable table, String taskType) {
        if (JudgeTaskType.RANKING_ROUND.name().equals(taskType) || JudgeTaskType.RANKING_PARTICIPANT.name().equals(taskType)) {
            return Math.toIntExact(roundResultMapper.selectCount(new LambdaQueryWrapper<RoundResult>()
                    .eq(RoundResult::getRoundTableId, table.getId())));
        }
        List<Long> entryIds = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundTableId, table.getId()))
                .stream()
                .map(RoundTableEntry::getBeerEntryId)
                .toList();
        if (entryIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<ScoreRecord> wrapper = new LambdaQueryWrapper<ScoreRecord>()
                .in(ScoreRecord::getBeerEntryId, entryIds);
        if (JudgeTaskType.CAPTAIN_FINALIZE.name().equals(taskType)) {
            wrapper.eq(ScoreRecord::getFinalFlag, FLAG_TRUE);
        } else {
            wrapper.eq(ScoreRecord::getJudgeAccountId, judgeId).eq(ScoreRecord::getFinalFlag, FLAG_FALSE);
        }
        return Math.toIntExact(scoreRecordMapper.selectCount(wrapper));
    }

    private RoundTableMember requireRoundTableMember(Long roundTableId, Long judgeId) {
        RoundTableMember member = roundTableMemberMapper.selectOne(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, roundTableId)
                .eq(RoundTableMember::getJudgeAccountId, judgeId));
        if (member == null) {
            throw new ForbiddenException("无权查看该轮次桌");
        }
        return member;
    }

    private RoundTableMember requireRankingCaptainMember(Long roundTableId, Long judgeId) {
        RoundTableMember member = roundTableMemberMapper.selectOne(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, roundTableId)
                .eq(RoundTableMember::getJudgeAccountId, judgeId)
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE));
        if (member == null || !JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
            throw new ForbiddenException("无权操作该轮次桌");
        }
        return member;
    }

    private boolean canSubmitRanking(RoundTableMember member, CompetitionRound round) {
        return RoundType.RANKING.name().equals(round.getRoundType())
                && isRankingEditableStatus(round.getStatus())
                && JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                && Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE);
    }

    private boolean canSubmitScoreRoundTable(RoundTableMember member, CompetitionRound round, RoundTable table) {
        return RoundType.SCORE.name().equals(round.getRoundType())
                && RoundStatus.PUBLISHED.name().equals(round.getStatus())
                && RoundStatus.PUBLISHED.name().equals(table.getStatus())
                && JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                && Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE);
    }

    private boolean isScoreRoundVisibleStatus(String status) {
        return RoundStatus.PUBLISHED.name().equals(status)
                || RoundStatus.SUBMITTED.name().equals(status)
                || RoundStatus.LOCKED.name().equals(status);
    }

    private boolean isRankingVisibleStatus(String status) {
        return RoundStatus.IN_PROGRESS.name().equals(status)
                || RoundStatus.SUBMITTED.name().equals(status)
                || RoundStatus.LOCKED.name().equals(status);
    }

    private boolean isRankingEditableStatus(String status) {
        return RoundStatus.IN_PROGRESS.name().equals(status)
                || RoundStatus.SUBMITTED.name().equals(status);
    }

    private ScoreConfirmationVO buildScoreConfirmation(RoundTable table, RoundTableMember currentMember) {
        List<RoundTableEntry> tableEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, table.getId())
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
        Set<Long> entryIds = tableEntries.stream().map(RoundTableEntry::getBeerEntryId).collect(Collectors.toSet());
        Map<Long, BeerEntry> entryById = loadEntries(entryIds);
        Map<Long, String> categoryNameById = listCategoryNames(table.getCompetitionId());
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entryIds);
        Map<Long, ScoreRecord> finalScoreByEntry = entryIds.isEmpty()
                ? Map.of()
                : scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .in(ScoreRecord::getBeerEntryId, entryIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        int version = currentResultVersion(table);
        Long judgeId = BaseContext.getCurrentId();
        boolean mineConfirmed = judgeId != null && roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getJudgeAccountId, judgeId)
                .eq(RoundTableConfirmation::getResultVersion, version)) > 0;
        List<ScoreConfirmationEntryVO> entries = tableEntries.stream()
                .map(item -> {
                    BeerEntry entry = entryById.get(item.getBeerEntryId());
                    ScoreRecord finalScore = finalScoreByEntry.get(item.getBeerEntryId());
                    EntryScanLabel label = labelByEntryId.get(item.getBeerEntryId());
                    return ScoreConfirmationEntryVO.builder()
                            .beerEntryId(item.getBeerEntryId())
                            .uuid(entry == null ? "" : entry.getUuid())
                            .shortCode(label == null ? "" : label.getShortCode())
                            .categoryName(entry == null ? "" : categoryNameById.getOrDefault(entry.getCategoryId(), ""))
                            .style(entry == null ? "" : entry.getStyle())
                            .consensusScore(finalScore == null ? null : finalScore.getConsensusScore())
                            .comments(finalScore == null ? "" : finalScore.getComments())
                            .advanced(finalScore != null && Objects.equals(finalScore.getAdvancedFlag(), FLAG_TRUE))
                            .build();
                })
                .toList();
        return ScoreConfirmationVO.builder()
                .roundTableId(table.getId())
                .tableName(table.getTableName())
                .status(table.getStatus())
                .resultVersion(version)
                .confirmedCount(resolveConfirmationConfirmedCount(table))
                .requiredCount(resolveConfirmationRequiredCount(table))
                .mineConfirmed(mineConfirmed)
                .readyForConfirmation(isScoreRoundTableReady(table))
                .overrideFlag(Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE))
                .overrideReason(table.getConfirmationOverrideReason())
                .overrideTime(table.getConfirmationOverrideTime())
                .entries(entries)
                .build();
    }

    private List<RoundRankingSlotVO> buildMyRankingDraft(RoundTable table, Map<Long, BeerEntry> entryById, Long judgeId) {
        RoundJudgeRankingDraft draft = roundJudgeRankingDraftMapper.selectOne(new LambdaQueryWrapper<RoundJudgeRankingDraft>()
                .eq(RoundJudgeRankingDraft::getRoundTableId, table.getId())
                .eq(RoundJudgeRankingDraft::getJudgeAccountId, judgeId));
        if (draft == null || !StringUtils.hasText(draft.getRankingsJson())) {
            return emptyRankingSlots(table);
        }
        Map<Integer, RankingResultItemRequest> itemByRank = readRankingDraft(draft.getRankingsJson()).stream()
                .filter(item -> item.getRankNo() != null)
                .collect(Collectors.toMap(RankingResultItemRequest::getRankNo, Function.identity(), (left, right) -> right));
        int count = table.getTargetCount() == null ? 0 : table.getTargetCount();
        List<RoundRankingSlotVO> slots = new ArrayList<>();
        for (int rank = 1; rank <= count; rank++) {
            RankingResultItemRequest item = itemByRank.get(rank);
            BeerEntry entry = item == null ? null : entryById.get(item.getBeerEntryId());
            slots.add(RoundRankingSlotVO.builder()
                    .rank(rank)
                    .label(item == null || !StringUtils.hasText(item.getSlotLabel()) ? defaultSlotLabel(table.getTargetMode(), rank) : item.getSlotLabel())
                    .uuid(entry == null ? "" : entry.getUuid())
                    .beerEntryId(item == null ? null : item.getBeerEntryId())
                    .build());
        }
        return slots;
    }

    private List<RoundRankingSlotVO> emptyRankingSlots(RoundTable table) {
        int count = table.getTargetCount() == null ? 0 : table.getTargetCount();
        List<RoundRankingSlotVO> slots = new ArrayList<>();
        for (int rank = 1; rank <= count; rank++) {
            slots.add(RoundRankingSlotVO.builder()
                    .rank(rank)
                    .label(defaultSlotLabel(table.getTargetMode(), rank))
                    .uuid("")
                    .beerEntryId(null)
                    .build());
        }
        return slots;
    }

    private void validateScoreRoundTableReady(RoundTable table) {
        List<RoundTableEntry> tableEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, table.getId()));
        if (tableEntries.isEmpty()) {
            throw new BaseException("本桌没有酒款，不能提交结果");
        }
        Set<Long> entryIds = tableEntries.stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        Map<Long, ScoreRecord> finalScoreByEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .in(ScoreRecord::getBeerEntryId, entryIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        List<Long> missing = entryIds.stream()
                .filter(entryId -> !finalScoreByEntry.containsKey(entryId))
                .toList();
        if (!missing.isEmpty()) {
            throw new BaseException("还有酒款未完成桌长汇总");
        }
        long advancedCount = finalScoreByEntry.values().stream()
                .filter(score -> Objects.equals(score.getAdvancedFlag(), FLAG_TRUE))
                .count();
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        if (advancedCount != targetCount) {
            throw new BaseException("晋级数量必须等于目标数量 " + targetCount);
        }
    }

    private void validateScoreRoundTableConfirmations(RoundTable table) {
        if (Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)) {
            return;
        }
        int required = resolveConfirmationRequiredCount(table);
        if (required <= 0) {
            return;
        }
        int confirmed = resolveConfirmationConfirmedCount(table);
        if (confirmed < required) {
            throw new BaseException("同桌评审确认未完成，暂不能提交本桌结果");
        }
    }

    private int resolveConfirmationRequiredCount(RoundTable table) {
        return Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, table.getId())
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)
                .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name())));
    }

    private int resolveConfirmationConfirmedCount(RoundTable table) {
        Set<Long> requiredJudgeIds = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                        .eq(RoundTableMember::getRoundTableId, table.getId())
                        .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)
                        .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name()))
                .stream()
                .map(RoundTableMember::getJudgeAccountId)
                .collect(Collectors.toSet());
        if (requiredJudgeIds.isEmpty()) {
            return 0;
        }
        return Math.toIntExact(roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getResultVersion, currentResultVersion(table))
                .eq(RoundTableConfirmation::getStatus, "AGREED")
                .in(RoundTableConfirmation::getJudgeAccountId, requiredJudgeIds)));
    }

    private boolean isScoreConfirmationReady(RoundTable table) {
        if (Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)) {
            return true;
        }
        int required = resolveConfirmationRequiredCount(table);
        return required <= 0 || resolveConfirmationConfirmedCount(table) >= required;
    }

    private int currentResultVersion(RoundTable table) {
        return table.getResultVersion() == null ? 0 : table.getResultVersion();
    }

    private boolean isScoreRoundTableReady(RoundTable table) {
        List<RoundTableEntry> tableEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, table.getId()));
        if (tableEntries.isEmpty()) {
            return false;
        }
        Set<Long> entryIds = tableEntries.stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        Map<Long, ScoreRecord> finalScoreByEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .in(ScoreRecord::getBeerEntryId, entryIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        if (entryIds.stream().anyMatch(entryId -> !finalScoreByEntry.containsKey(entryId))) {
            return false;
        }
        long advancedCount = finalScoreByEntry.values().stream()
                .filter(score -> Objects.equals(score.getAdvancedFlag(), FLAG_TRUE))
                .count();
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        return advancedCount == targetCount;
    }

    private void validateRankingDraft(RoundTable table, RankingDraftSaveRequest request) {
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        if (request.getResults().size() > targetCount) {
            throw new BaseException("参考排序数量不能超过槽位数量");
        }
        Set<Long> tableEntryIds = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundTableId, table.getId()))
                .stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        Set<Long> resultEntryIds = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        for (RankingResultItemRequest item : request.getResults()) {
            if (item == null) {
                throw new BaseException("参考排序项不能为空");
            }
            if (!tableEntryIds.contains(item.getBeerEntryId())) {
                throw new BaseException("参考排序酒款不属于当前桌");
            }
            if (!resultEntryIds.add(item.getBeerEntryId())) {
                throw new BaseException("参考排序酒款不能重复");
            }
            if (!ranks.add(item.getRankNo())) {
                throw new BaseException("参考排序名次不能重复");
            }
            if (item.getRankNo() < 1 || item.getRankNo() > targetCount) {
                throw new BaseException("参考排序名次超出槽位范围");
            }
        }
    }

    private String writeRankingDraft(List<RankingResultItemRequest> results) {
        try {
            return objectMapper.writeValueAsString(results == null ? List.of() : results);
        } catch (JsonProcessingException ex) {
            throw new BaseException("参考排序保存失败");
        }
    }

    private List<RankingResultItemRequest> readRankingDraft(String rankingsJson) {
        try {
            return objectMapper.readValue(rankingsJson, new TypeReference<List<RankingResultItemRequest>>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private List<RoundRankingSlotVO> buildRankings(RoundTable table, List<RoundResult> results, Map<Long, BeerEntry> entryById) {
        Map<Integer, RoundResult> resultByRank = results.stream()
                .filter(result -> result.getRankNo() != null)
                .collect(Collectors.toMap(RoundResult::getRankNo, Function.identity(), (left, right) -> right));
        int count = table.getTargetCount() == null ? 0 : table.getTargetCount();
        List<RoundRankingSlotVO> slots = new ArrayList<>();
        for (int rank = 1; rank <= count; rank++) {
            RoundResult result = resultByRank.get(rank);
            BeerEntry entry = result == null ? null : entryById.get(result.getBeerEntryId());
            slots.add(RoundRankingSlotVO.builder()
                    .rank(rank)
                    .label(result == null || !StringUtils.hasText(result.getSlotLabel()) ? defaultSlotLabel(table.getTargetMode(), rank) : result.getSlotLabel())
                    .uuid(entry == null ? "" : entry.getUuid())
                    .beerEntryId(result == null ? null : result.getBeerEntryId())
                    .build());
        }
        return slots;
    }

    private String defaultSlotLabel(String targetMode, int rank) {
        if (RoundTargetMode.MEDALS.name().equals(targetMode)) {
            return switch (rank) {
                case 1 -> SLOT_GOLD;
                case 2 -> SLOT_SILVER;
                case 3 -> SLOT_BRONZE;
                default -> "第 " + rank + " 名";
            };
        }
        if (RoundTargetMode.CHAMPION.name().equals(targetMode)) {
            return "总冠军";
        }
        return "第 " + rank + " 名";
    }

    private String resolveSlotLabel(RoundResult result) {
        if (StringUtils.hasText(result.getSlotLabel())) {
            return result.getSlotLabel();
        }
        if (result.getRankNo() != null) {
            return "第 " + result.getRankNo() + " 名";
        }
        if (RoundResultType.ADVANCE.name().equals(result.getResultType())) {
            return "晋级";
        }
        return result.getResultType();
    }

    private List<CompetitionRound> listRounds(Long competitionId) {
        return competitionRoundMapper.selectList(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .orderByAsc(CompetitionRound::getSortOrder)
                .orderByAsc(CompetitionRound::getRoundNo)
                .orderByAsc(CompetitionRound::getId));
    }

    private List<RoundTable> listRoundTables(Long roundId) {
        return roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getRoundId, roundId)
                .orderByAsc(RoundTable::getSortOrder)
                .orderByAsc(RoundTable::getId));
    }

    private List<RoundTableEntry> listRoundEntries(Long roundId) {
        return roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, roundId)
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
    }

    private List<RoundTable> listRoundTablesByCompetition(Long competitionId) {
        return roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getCompetitionId, competitionId)
                .orderByAsc(RoundTable::getSortOrder)
                .orderByAsc(RoundTable::getId));
    }

    private List<RoundTableEntry> listRoundEntriesByCompetition(Long competitionId) {
        return roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getCompetitionId, competitionId)
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
    }

    private List<RoundResult> listResultsByCompetition(Long competitionId) {
        return roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getCompetitionId, competitionId)
                .orderByAsc(RoundResult::getRankNo)
                .orderByAsc(RoundResult::getId));
    }

    private List<RoundResult> listCandidateResults(Long sourceRoundId) {
        return roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundId, sourceRoundId)
                .in(RoundResult::getResultType, RoundResultType.ADVANCE.name(), RoundResultType.RANK.name(), RoundResultType.AWARD_CANDIDATE.name(), RoundResultType.CHAMPION.name())
                .orderByAsc(RoundResult::getRankNo)
                .orderByAsc(RoundResult::getId));
    }

    private List<RoundResult> resolveCandidateResultsForRound(CompetitionRound round, List<RoundTable> tables) {
        if (round.getSourceRoundId() == null) {
            return List.of();
        }
        String targetMode = tables.stream()
                .map(RoundTable::getTargetMode)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(RoundTargetMode.TOP_N.name());
        return filterCandidatesForTargetMode(listCandidateResults(round.getSourceRoundId()), targetMode);
    }

    private List<RoundResult> filterCandidatesForTargetMode(List<RoundResult> candidates, String targetMode) {
        if (!RoundTargetMode.CHAMPION.name().equals(targetMode)) {
            return candidates;
        }
        Map<Long, RoundTable> sourceTables = new LinkedHashMap<>();
        candidates.stream()
                .map(RoundResult::getRoundTableId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(tableId -> {
                    RoundTable table = roundTableMapper.selectById(tableId);
                    if (table != null) {
                        sourceTables.put(tableId, table);
                    }
                });
        return candidates.stream()
                .filter(result -> {
                    RoundTable sourceTable = sourceTables.get(result.getRoundTableId());
                    return sourceTable != null
                            && RoundTargetMode.MEDALS.name().equals(sourceTable.getTargetMode())
                            && (Objects.equals(result.getRankNo(), 1) || SLOT_GOLD.equals(result.getSlotLabel()));
                })
                .toList();
    }

    private List<JudgeTable> listBaseTables(Long competitionId) {
        return judgeTableMapper.selectList(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, competitionId)
                .orderByAsc(JudgeTable::getSortOrder)
                .orderByAsc(JudgeTable::getId));
    }

    private List<JudgeAssignment> listBaseAssignments(Long competitionId) {
        return judgeAssignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId));
    }

    private List<BeerEntry> listStoredEntries(Long competitionId) {
        return beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .eq(BeerEntry::getStoredFlag, FLAG_TRUE)
                .ne(BeerEntry::getStatus, "CANCELED")
                .orderByAsc(BeerEntry::getId));
    }

    private List<RoundTableMember> listMembers(List<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return List.of();
        }
        return roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .in(RoundTableMember::getRoundTableId, tableIds));
    }

    private Map<Long, String> listCategoryNames(Long competitionId) {
        return competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId))
                .stream()
                .collect(Collectors.toMap(CompetitionCategory::getId, CompetitionCategory::getName, (left, right) -> left));
    }

    private Map<Long, RoundResult> latestResultByEntry(Long competitionId) {
        Map<Long, RoundResult> map = new LinkedHashMap<>();
        roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                        .eq(RoundResult::getCompetitionId, competitionId)
                        .orderByAsc(RoundResult::getRoundId)
                        .orderByAsc(RoundResult::getRankNo)
                        .orderByAsc(RoundResult::getId))
                .forEach(result -> map.put(result.getBeerEntryId(), result));
        return map;
    }

    private Map<Long, BeerEntry> loadEntries(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return beerEntryMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(BeerEntry::getId, Function.identity()));
    }

    private Map<String, BeerEntry> loadEntryByUuids(Long competitionId, Set<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return Map.of();
        }
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .in(BeerEntry::getUuid, uuids));
        if (entries.size() != uuids.size()) {
            throw new BaseException("存在不属于当前比赛的酒款");
        }
        return entries.stream().collect(Collectors.toMap(BeerEntry::getUuid, Function.identity()));
    }

    private Map<Long, JudgeAccount> loadJudges(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return judgeAccountMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(JudgeAccount::getId, Function.identity()));
    }

    private Map<String, JudgeAccount> loadJudgeByPublicIds(Set<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return Map.of();
        }
        List<JudgeAccount> judges = judgeAccountMapper.selectList(new LambdaQueryWrapper<JudgeAccount>()
                .in(JudgeAccount::getPublicId, publicIds));
        if (judges.size() != publicIds.size()) {
            throw new BaseException("存在无效桌长");
        }
        judges.forEach(judge -> {
            if (JudgeAccountStatus.of(judge.getStatus()) != JudgeAccountStatus.ACTIVE) {
                throw new BaseException("桌长必须是启用评审：" + judge.getName());
            }
        });
        return judges.stream().collect(Collectors.toMap(JudgeAccount::getPublicId, Function.identity()));
    }

    private Map<Long, RoundTable> loadRoundTables(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return roundTableMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(RoundTable::getId, Function.identity()));
    }

    private Map<Long, CompetitionRound> loadRounds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return competitionRoundMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(CompetitionRound::getId, Function.identity()));
    }

    private Map<Long, Competition> loadCompetitions(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return competitionMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Competition::getId, Function.identity()));
    }

    private List<JudgeAccount> resolveCaptains(List<String> publicIds, Integer tableCount) {
        List<String> normalized = safeList(publicIds).stream().filter(StringUtils::hasText).toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (normalized.size() > tableCount) {
            throw new BaseException("桌长数量不能超过桌数");
        }
        return new ArrayList<>(loadJudgeByPublicIds(new LinkedHashSet<>(normalized)).values());
    }

    private Competition requireCompetition(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private CompetitionStatus parseCompetitionStatus(Competition competition) {
        try {
            return CompetitionStatus.valueOf(competition.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BaseException("比赛状态不合法：" + competition.getStatus());
        }
    }

    private void validateCompetitionStageForRoundAllocation(Competition competition, CompetitionRound round) {
        CompetitionStatus status = parseCompetitionStatus(competition);
        if (RoundType.SCORE.name().equals(round.getRoundType()) && !isPreJudgingStage(status)) {
            throw new BaseException("评审已开始，不能再保存第一轮编排");
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && !isRankingRoundStage(status)) {
            throw new BaseException("当前阶段不能保存排序轮编排");
        }
    }

    private void validateCompetitionStageForRoundPublish(Competition competition, CompetitionRound round) {
        CompetitionStatus status = parseCompetitionStatus(competition);
        if (RoundType.SCORE.name().equals(round.getRoundType()) && status != CompetitionStatus.JUDGING_PREP) {
            throw new BaseException("第一轮只能在评审准备阶段发布");
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && !isRankingRoundStage(status)) {
            throw new BaseException("当前阶段不能发布排序轮");
        }
    }

    private boolean isRankingRoundStage(CompetitionStatus status) {
        return status == CompetitionStatus.JUDGING
                || status == CompetitionStatus.RESULT_CONFIRMING;
    }

    private boolean isPreJudgingStage(CompetitionStatus status) {
        return status == CompetitionStatus.DRAFT
                || status == CompetitionStatus.REGISTRATION_OPEN
                || status == CompetitionStatus.REGISTRATION_CLOSED
                || status == CompetitionStatus.JUDGING_PREP;
    }

    private void validateSourceIsLatestLockedRound(Long competitionId, CompetitionRound sourceRound) {
        CompetitionRound latestLocked = findLastLockedRound(competitionId);
        if (latestLocked == null || !latestLocked.getId().equals(sourceRound.getId())) {
            throw new BaseException("只能基于最新锁定轮次创建下一轮");
        }
    }

    private CompetitionRound findLastLockedRound(Long competitionId) {
        return listRounds(competitionId).stream()
                .filter(round -> RoundStatus.LOCKED.name().equals(round.getStatus()))
                .max(Comparator.comparing(CompetitionRound::getRoundNo))
                .orElse(null);
    }

    private boolean isAwardRound(CompetitionRound round) {
        List<RoundTable> tables = listRoundTables(round.getId());
        return RoundType.RANKING.name().equals(round.getRoundType())
                && !tables.isEmpty()
                && tables.stream().allMatch(table -> RoundTargetMode.MEDALS.name().equals(table.getTargetMode())
                || RoundTargetMode.CHAMPION.name().equals(table.getTargetMode()));
    }

    private boolean isTerminalRound(CompetitionRound round) {
        List<RoundTable> tables = listRoundTables(round.getId());
        return RoundType.RANKING.name().equals(round.getRoundType())
                && !tables.isEmpty()
                && tables.stream().allMatch(table -> RoundTargetMode.CHAMPION.name().equals(table.getTargetMode()));
    }

    private CompetitionRound requireRound(Long competitionId, Long roundId) {
        CompetitionRound round = competitionRoundMapper.selectById(roundId);
        if (round == null || !round.getCompetitionId().equals(competitionId)) {
            throw new ResourceNotFoundException("轮次不存在");
        }
        return round;
    }

    private RoundTable requireRoundTable(Long roundTableId) {
        RoundTable table = roundTableMapper.selectById(roundTableId);
        if (table == null) {
            throw new ResourceNotFoundException("轮次桌不存在");
        }
        return table;
    }

    private JudgeAccount requireActiveJudge(Long judgeId) {
        JudgeAccount judge = judgeAccountMapper.selectById(judgeId);
        if (judge == null) {
            throw new ResourceNotFoundException("评审账号不存在");
        }
        if (JudgeAccountStatus.of(judge.getStatus()) != JudgeAccountStatus.ACTIVE) {
            throw new ForbiddenException("评审账号未启用");
        }
        return judge;
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

    private Long findCaptainId(List<JudgeAssignment> assignments) {
        return assignments.stream()
                .filter(assignment -> JudgeRoleType.CAPTAIN.name().equals(assignment.getRole()))
                .map(JudgeAssignment::getJudgeAccountId)
                .findFirst()
                .orElse(null);
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

    private String resolveTargetMode(CompetitionRound round, String requested) {
        if (StringUtils.hasText(requested)) {
            return RoundTargetMode.of(requested).name();
        }
        return RoundType.SCORE.name().equals(round.getRoundType())
                ? RoundTargetMode.ADVANCE_COUNT.name()
                : RoundTargetMode.TOP_N.name();
    }

    private int resolveJudgeProgress(RoundTable table, List<RoundTableEntry> entries) {
        if (entries.isEmpty()) {
            return 0;
        }
        List<Long> entryIds = entries.stream().map(RoundTableEntry::getBeerEntryId).toList();
        int taskJudges = Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, table.getId())
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)));
        if (table.getCaptainJudgeId() != null) {
            boolean captainCounted = roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                    .eq(RoundTableMember::getRoundTableId, table.getId())
                    .eq(RoundTableMember::getJudgeAccountId, table.getCaptainJudgeId())
                    .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)) > 0;
            if (!captainCounted) {
                taskJudges++;
            }
        }
        if (taskJudges <= 0) {
            return 0;
        }
        int submitted = Math.toIntExact(scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .in(ScoreRecord::getBeerEntryId, entryIds)
                .eq(ScoreRecord::getFinalFlag, FLAG_FALSE)));
        int total = entries.size() * taskJudges;
        return Math.min(FULL_PROGRESS, submitted * FULL_PROGRESS / total);
    }

    private int countMembers(List<RoundTableMember> members, String role) {
        return (int) members.stream().filter(member -> role.equals(member.getRole())).count();
    }

    private String roleLabel(String role) {
        if (JudgeRoleType.CAPTAIN.name().equals(role)) {
            return "桌长";
        }
        if (JudgeRoleType.PROFESSIONAL.name().equals(role)) {
            return "专业评审";
        }
        if (JudgeRoleType.CROSS.name().equals(role)) {
            return "跨界评审";
        }
        return role;
    }

    private String roleLabel(RoundTableMember member) {
        if (!JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                && Objects.equals(member.getSystemTaskRequired(), FLAG_FALSE)) {
            return "参与评审";
        }
        return roleLabel(member.getRole());
    }

    private String roleLabel(CompetitionRound round, RoundTableMember member) {
        if (RoundType.RANKING.name().equals(round.getRoundType())
                && !JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
            return "参与评审";
        }
        return roleLabel(member);
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? List.of() : source;
    }
}
