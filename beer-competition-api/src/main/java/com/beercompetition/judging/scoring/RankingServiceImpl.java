package com.beercompetition.judging.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundJudgeRankingDraftMapper;
import com.beercompetition.mapper.RoundTableConfirmationMapper;
import com.beercompetition.mapper.RoundTableCategoryStateMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.RankingDraftSaveRequest;
import com.beercompetition.pojo.dto.RankingResultItemRequest;
import com.beercompetition.pojo.dto.RankingSubmitRequest;
import com.beercompetition.pojo.dto.RoundTableConfirmationRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundEntryStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundJudgeRankingDraft;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableConfirmation;
import com.beercompetition.pojo.po.RoundTableCategoryState;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.RankingConfirmationSlotVO;
import com.beercompetition.pojo.vo.RankingConfirmationVO;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import com.beercompetition.service.impl.round.RoundValidationPolicy;
import com.beercompetition.judging.assignment.RoundCandidateSyncService;
import com.beercompetition.judging.round.RoundTableCategoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_FALSE;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_TRUE;
import static com.beercompetition.service.impl.round.RoundConstants.SLOT_BRONZE;
import static com.beercompetition.service.impl.round.RoundConstants.SLOT_GOLD;
import static com.beercompetition.service.impl.round.RoundConstants.SLOT_SILVER;
import com.beercompetition.judging.scoring.RankingService;

/**
 * 保存排名草稿并完成提交、确认和最终锁定。
 */
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final CompetitionMapper competitionMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMapper roundTableMapper;

    private final RoundTableMemberMapper roundTableMemberMapper;

    private final RoundTableEntryMapper roundTableEntryMapper;

    private final RoundResultMapper roundResultMapper;

    private final RoundTableConfirmationMapper roundTableConfirmationMapper;

    private final RoundTableCategoryStateMapper roundTableCategoryStateMapper;

    private final RoundJudgeRankingDraftMapper roundJudgeRankingDraftMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final ObjectMapper objectMapper;

    private final RoundQuerySupport roundQuerySupport;

    private final RoundValidationPolicy roundValidationPolicy;

    private final RoundCandidateSyncService roundCandidateSyncService;

    private final RoundTableCategoryService roundTableCategoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRanking(Long roundTableId, RankingSubmitRequest request) {
        // 1) 查询排序桌并校验桌长权限
        Long judgeId = BaseContext.getCurrentId();
        roundQuerySupport.requireActiveJudge(judgeId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (!RoundType.RANKING.name().equals(round.getRoundType()) || !isRankingEditableStatus(round.getStatus())) {
            throw new BaseException("当前轮次不能提交排序");
        }
        if (RoundStatus.LOCKED.name().equals(table.getStatus())) {
            throw new BaseException("本桌排序已锁定，不能调整");
        }
        assertCompetitionNotArchived(table.getCompetitionId());
        requireRankingCaptainMember(roundTableId, judgeId);
        Long categoryId = roundTableCategoryService.resolveCategoryId(table, request.getCategoryId());
        roundValidationPolicy.validateRankingSubmit(table, request, categoryId);

        // 2) 写入待确认排序结果和桌内酒款状态
        RoundTableCategoryState categoryState = RoundTargetMode.MEDALS.name().equals(table.getTargetMode())
                ? roundTableCategoryService.requireState(table, categoryId) : null;
        int nextVersion = categoryState == null ? currentResultVersion(table) + 1 : currentResultVersion(categoryState) + 1;
        LambdaQueryWrapper<RoundResult> oldResults = new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundTableId, roundTableId);
        if (categoryId != null) oldResults.eq(RoundResult::getCategoryId, categoryId);
        roundResultMapper.delete(oldResults);
        Set<Long> rankedEntryIds = request.getResults().stream().map(RankingResultItemRequest::getBeerEntryId).collect(Collectors.toSet());
        for (RankingResultItemRequest item : request.getResults()) {
            int rank = item.getRankNo();
            roundResultMapper.insert(RoundResult.builder()
                    .competitionId(table.getCompetitionId())
                    .roundId(table.getRoundId())
                    .roundTableId(roundTableId)
                    .categoryId(categoryId)
                    .beerEntryId(item.getBeerEntryId())
                    .resultType(RoundResultType.RANK.name())
                    .rankNo(rank)
                    .slotLabel(StringUtils.hasText(item.getSlotLabel()) ? item.getSlotLabel().trim() : defaultSlotLabel(table.getTargetMode(), rank))
                    .submittedBy(judgeId)
                    .submittedTime(LocalDateTime.now())
                    .lockedFlag(FLAG_FALSE)
                    .build());
        }
        LambdaQueryWrapper<RoundTableEntry> entriesQuery = new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, roundTableId);
        if (categoryId != null) {
            Set<Long> categoryEntryIds = roundQuerySupport.loadEntries(roundTableEntryMapper.selectList(entriesQuery).stream()
                            .map(RoundTableEntry::getBeerEntryId).collect(Collectors.toSet())).values().stream()
                    .filter(entry -> Objects.equals(entry.getCategoryId(), categoryId))
                    .map(BeerEntry::getId).collect(Collectors.toSet());
            entriesQuery.in(RoundTableEntry::getBeerEntryId, categoryEntryIds);
        }
        roundTableEntryMapper.selectList(entriesQuery)
                .forEach(entry -> {
                    entry.setStatus(rankedEntryIds.contains(entry.getBeerEntryId()) ? RoundEntryStatus.RANKED.name() : RoundEntryStatus.ELIMINATED.name());
                    roundTableEntryMapper.updateById(entry);
                });

        // 3) 开启新的同桌确认版本
        if (categoryState != null) {
            categoryState.setStatus(RoundStatus.IN_PROGRESS.name());
            categoryState.setResultVersion(nextVersion);
            roundTableCategoryStateMapper.updateById(categoryState);
        }
        // updateById 默认跳过 null 字段，覆盖标记需用显式 set 才能清空。
        roundTableMapper.update(null, new LambdaUpdateWrapper<RoundTable>()
                .eq(RoundTable::getId, table.getId())
                .set(RoundTable::getStatus, RoundStatus.IN_PROGRESS.name())
                .set(RoundTable::getResultVersion, nextVersion)
                .set(RoundTable::getConfirmationOverrideFlag, FLAG_FALSE)
                .set(RoundTable::getConfirmationOverrideReason, null)
                .set(RoundTable::getConfirmationOverrideBy, null)
                .set(RoundTable::getConfirmationOverrideTime, null));
        if (RoundStatus.SUBMITTED.name().equals(round.getStatus())) {
            competitionRoundMapper.update(null, new LambdaUpdateWrapper<CompetitionRound>()
                    .eq(CompetitionRound::getId, round.getId())
                    .set(CompetitionRound::getStatus, RoundStatus.IN_PROGRESS.name())
                    .set(CompetitionRound::getSubmittedTime, null));
        }
        autoSubmitRoundTableIfReady(roundTableMapper.selectById(roundTableId), competitionRoundMapper.selectById(round.getId()));
    }

    @Override
    public RankingConfirmationVO getRankingConfirmation(Long roundTableId) {
        return getRankingConfirmation(roundTableId, null);
    }

    @Override
    public RankingConfirmationVO getRankingConfirmation(Long roundTableId, Long requestedCategoryId) {
        // 1) 查询排序桌并校验查看权限
        Long judgeId = BaseContext.getCurrentId();
        roundQuerySupport.requireActiveJudge(judgeId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !RoundType.RANKING.name().equals(round.getRoundType()) || !isRankingVisibleStatus(round.getStatus())) {
            throw new BaseException("当前排序轮次不可查看");
        }
        assertCompetitionNotArchived(table.getCompetitionId());
        RoundTableMember member = requireRoundTableMember(roundTableId, judgeId);

        // 2) 组装本桌排序确认结果
        Long categoryId = roundTableCategoryService.resolveCategoryId(table, requestedCategoryId);
        return buildRankingConfirmation(table, member, categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RankingConfirmationVO confirmRankingRoundTable(Long roundTableId, RoundTableConfirmationRequest request) {
        // 1) 查询排序桌并校验评委确认权限
        Long judgeId = BaseContext.getCurrentId();
        roundQuerySupport.requireActiveJudge(judgeId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !RoundType.RANKING.name().equals(round.getRoundType()) || !isRankingEditableStatus(round.getStatus())) {
            throw new BaseException("当前轮次不能确认本桌排序");
        }
        if (RoundStatus.LOCKED.name().equals(table.getStatus()) || RoundStatus.SUBMITTED.name().equals(table.getStatus())) {
            throw new BaseException("本桌排序已提交或已锁定");
        }
        assertCompetitionNotArchived(table.getCompetitionId());
        RoundTableMember member = requireRoundTableMember(roundTableId, judgeId);
        if ("REMOVED".equalsIgnoreCase(member.getStatus())
                || JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
            throw new ForbiddenException("当前账号不需要确认本桌排序");
        }
        Long categoryId = roundTableCategoryService.resolveCategoryId(table, request.getCategoryId());
        roundValidationPolicy.validateRankingRoundTableReady(table, categoryId);

        // 2) 写入当前版本确认记录
        int version = currentResultVersionFor(table, categoryId);
        validateConfirmationVersion(request, version);
        RoundTableConfirmation existing = roundTableConfirmationMapper.selectOne(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, roundTableId)
                .eq(RoundTableConfirmation::getCategoryId, categoryId)
                .eq(RoundTableConfirmation::getJudgeAccountId, judgeId)
                .eq(RoundTableConfirmation::getResultVersion, version));
        if (existing == null) {
            roundTableConfirmationMapper.insert(RoundTableConfirmation.builder()
                    .roundTableId(roundTableId)
                    .categoryId(categoryId)
                    .judgeAccountId(judgeId)
                    .resultVersion(version)
                    .status("AGREED")
                    .confirmedTime(LocalDateTime.now())
                    .build());
        }

        // 3) 当前版本确认完成后自动提交本桌排序
        autoSubmitRoundTableIfReady(roundTableMapper.selectById(roundTableId), round);
        return buildRankingConfirmation(roundTableMapper.selectById(roundTableId), member, categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalizeRanking(Long roundTableId) {
        // 1) 查询排序桌并校验桌长最终提交权限
        Long judgeId = BaseContext.getCurrentId();
        roundQuerySupport.requireActiveJudge(judgeId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !RoundType.RANKING.name().equals(round.getRoundType()) || !isRankingEditableStatus(round.getStatus())) {
            throw new BaseException("当前轮次不能提交排序");
        }
        if (RoundStatus.SUBMITTED.name().equals(table.getStatus()) || RoundStatus.LOCKED.name().equals(table.getStatus())) {
            throw new BaseException("本桌排序已提交或已锁定，不能提交");
        }
        assertCompetitionNotArchived(table.getCompetitionId());
        requireRankingCaptainMember(roundTableId, judgeId);
        roundValidationPolicy.validateRankingRoundTableReady(table);
        validateRankingRoundTableConfirmations(table);

        // 2) 更新桌和轮次提交状态
        table.setStatus(RoundStatus.SUBMITTED.name());
        roundTableMapper.updateById(table);
        if (roundQuerySupport.listRoundTables(round.getId()).stream().allMatch(item -> RoundStatus.SUBMITTED.name().equals(item.getStatus()))) {
            round.setStatus(RoundStatus.SUBMITTED.name());
            round.setSubmittedTime(LocalDateTime.now());
            competitionRoundMapper.updateById(round);
        }
        roundCandidateSyncService.syncDependentDrafts(round);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRankingDraft(Long roundTableId, RankingDraftSaveRequest request) {
        // 1) 校验当前评审可以为本桌保存个人参考排序
        Long judgeId = BaseContext.getCurrentId();
        roundQuerySupport.requireActiveJudge(judgeId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !RoundType.RANKING.name().equals(round.getRoundType()) || !isRankingEditableStatus(round.getStatus())) {
            throw new BaseException("当前轮次不能保存参考排序");
        }
        if (RoundStatus.LOCKED.name().equals(table.getStatus())) {
            throw new BaseException("本桌排序已锁定，不能调整参考排序");
        }
        assertCompetitionNotArchived(table.getCompetitionId());
        RoundTableMember member = requireRoundTableMember(roundTableId, judgeId);
        if ("REMOVED".equalsIgnoreCase(member.getStatus())) {
            throw new ForbiddenException("当前账号已离场，不能继续调整参考排序");
        }
        Long categoryId = roundTableCategoryService.resolveCategoryId(table, request.getCategoryId());
        roundValidationPolicy.validateRankingDraft(table, categoryId, request.getResults());

        // 2) 用覆盖写方式保存草稿，保证同一评审同一桌只有一份参考排序
        roundJudgeRankingDraftMapper.delete(new LambdaQueryWrapper<RoundJudgeRankingDraft>()
                .eq(RoundJudgeRankingDraft::getRoundTableId, roundTableId)
                .eq(RoundJudgeRankingDraft::getCategoryId, categoryId)
                .eq(RoundJudgeRankingDraft::getJudgeAccountId, judgeId));
        roundJudgeRankingDraftMapper.insert(RoundJudgeRankingDraft.builder()
                .competitionId(table.getCompetitionId())
                .roundId(table.getRoundId())
                .roundTableId(roundTableId)
                .categoryId(categoryId)
                .judgeAccountId(judgeId)
                .rankingsJson(writeRankingDraft(request.getResults()))
                .build());
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

    private void assertCompetitionNotArchived(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition != null && CompetitionStatus.ARCHIVED.name().equals(competition.getStatus())) {
            throw new BaseException("比赛已归档");
        }
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

    private RankingConfirmationVO buildRankingConfirmation(RoundTable table, RoundTableMember currentMember, Long categoryId) {
        LambdaQueryWrapper<RoundResult> resultQuery = new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundTableId, table.getId())
                .orderByAsc(RoundResult::getRankNo);
        if (categoryId != null) resultQuery.eq(RoundResult::getCategoryId, categoryId);
        else resultQuery.isNull(RoundResult::getCategoryId);
        List<RoundResult> results = roundResultMapper.selectList(resultQuery);
        Set<Long> entryIds = results.stream().map(RoundResult::getBeerEntryId).collect(Collectors.toSet());
        Map<Long, BeerEntry> entryById = roundQuerySupport.loadEntries(entryIds);
        Map<Long, String> categoryNameById = roundQuerySupport.listCategoryNames(table.getCompetitionId());
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entryIds);
        Long judgeId = currentMember == null ? null : currentMember.getJudgeAccountId();
        int version = currentResultVersionFor(table, categoryId);
        LambdaQueryWrapper<RoundTableConfirmation> mineQuery = new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getJudgeAccountId, judgeId)
                .eq(RoundTableConfirmation::getResultVersion, version);
        if (categoryId != null) mineQuery.eq(RoundTableConfirmation::getCategoryId, categoryId);
        else mineQuery.isNull(RoundTableConfirmation::getCategoryId);
        boolean mineConfirmed = judgeId != null && roundTableConfirmationMapper.selectCount(mineQuery) > 0;
        List<RankingConfirmationSlotVO> slots = results.stream()
                .map(result -> {
                    BeerEntry entry = entryById.get(result.getBeerEntryId());
                    EntryScanLabel label = labelByEntryId.get(result.getBeerEntryId());
                    return RankingConfirmationSlotVO.builder()
                            .rank(result.getRankNo())
                            .label(resolveSlotLabel(result))
                            .beerEntryId(result.getBeerEntryId())
                            .uuid(entry == null ? "" : entry.getUuid())
                            .shortCode(label == null ? "" : label.getShortCode())
                            .categoryName(entry == null ? "" : categoryNameById.getOrDefault(entry.getCategoryId(), ""))
                            .style(entry == null ? "" : entry.getStyle())
                            .build();
                })
                .toList();
        return RankingConfirmationVO.builder()
                .roundTableId(table.getId())
                .categoryId(categoryId)
                .categoryName(resolveCategoryName(table, categoryId))
                .tableName(table.getTableName())
                .status(table.getStatus())
                .targetMode(table.getTargetMode())
                .resultVersion(version)
                .confirmedCount(resolveRankingConfirmationConfirmedCount(table, categoryId))
                .requiredCount(resolveRankingConfirmationRequiredCount(table, categoryId))
                .mineConfirmed(mineConfirmed)
                .readyForConfirmation(roundValidationPolicy.isRankingRoundTableReady(table, categoryId))
                .readyForFinalSubmit(isRankingConfirmationReady(table, categoryId))
                .overrideFlag(Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE))
                .overrideReason(table.getConfirmationOverrideReason())
                .overrideTime(table.getConfirmationOverrideTime())
                .slots(slots)
                .build();
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
        if (isFeedbackOnlyCompetition(table.getCompetitionId())) {
            return;
        }
        long advancedCount = finalScoreByEntry.values().stream()
                .filter(score -> Objects.equals(score.getAdvancedFlag(), FLAG_TRUE))
                .count();
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        // 允许实际晋级少于目标数量（缺额），但不能超过。
        if (advancedCount > targetCount) {
            throw new BaseException("晋级数量不能超过目标数量 " + targetCount);
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

    private int resolveRankingConfirmationRequiredCount(RoundTable table, Long categoryId) {
        return Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, table.getId())
                .ne(RoundTableMember::getStatus, "REMOVED")
                .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name())));
    }

    private int resolveRankingConfirmationConfirmedCount(RoundTable table, Long categoryId) {
        Set<Long> requiredJudgeIds = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                        .eq(RoundTableMember::getRoundTableId, table.getId())
                        .ne(RoundTableMember::getStatus, "REMOVED")
                        .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name()))
                .stream()
                .map(RoundTableMember::getJudgeAccountId)
                .collect(Collectors.toSet());
        if (requiredJudgeIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<RoundTableConfirmation> query = new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getResultVersion, currentResultVersionFor(table, categoryId))
                .eq(RoundTableConfirmation::getStatus, "AGREED")
                .in(RoundTableConfirmation::getJudgeAccountId, requiredJudgeIds);
        if (categoryId == null) query.isNull(RoundTableConfirmation::getCategoryId);
        else query.eq(RoundTableConfirmation::getCategoryId, categoryId);
        return Math.toIntExact(roundTableConfirmationMapper.selectCount(query));
    }

    private void validateRankingRoundTableConfirmations(RoundTable table) {
        if (Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)) {
            return;
        }
        List<Long> categoryIds = RoundTargetMode.MEDALS.name().equals(table.getTargetMode())
                ? roundTableCategoryService.listCategoryIds(table) : java.util.Collections.singletonList(null);
        for (Long categoryId : categoryIds) {
            int required = resolveRankingConfirmationRequiredCount(table, categoryId);
            if (required > 0 && resolveRankingConfirmationConfirmedCount(table, categoryId) < required) {
                throw new BaseException("同桌评审确认未完成，暂不能提交本桌排序");
            }
        }
    }

    private void validateConfirmationVersion(RoundTableConfirmationRequest request, int currentVersion) {
        if (request == null || request.getResultVersion() == null || !Objects.equals(request.getResultVersion(), currentVersion)) {
            throw new BaseException("本桌结果已更新，请重新核对后确认");
        }
    }

    private void autoSubmitRoundTableIfReady(RoundTable table, CompetitionRound round) {
        if (table == null || round == null) {
            return;
        }
        if (RoundStatus.SUBMITTED.name().equals(table.getStatus()) || RoundStatus.LOCKED.name().equals(table.getStatus())) {
            return;
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            validateScoreRoundTableReady(table);
            if (!isScoreConfirmationReady(table)) {
                return;
            }
        } else if (RoundType.RANKING.name().equals(round.getRoundType())) {
            // 自动提交是尽力而为：多组别桌只提交了部分组别时不算就绪，直接返回等待，不能抛错中断调用方事务。
            if (!isRankingConfirmationReady(table)) {
                return;
            }
        } else {
            return;
        }
        table.setStatus(RoundStatus.SUBMITTED.name());
        roundTableMapper.updateById(table);
        if (roundQuerySupport.listRoundTables(round.getId()).stream().allMatch(item -> RoundStatus.SUBMITTED.name().equals(item.getStatus()))) {
            round.setStatus(RoundStatus.SUBMITTED.name());
            round.setSubmittedTime(LocalDateTime.now());
            competitionRoundMapper.updateById(round);
        }
        roundCandidateSyncService.syncDependentDrafts(round);
    }

    private boolean isRankingConfirmationReady(RoundTable table, Long categoryId) {
        if (!roundValidationPolicy.isRankingRoundTableReady(table, categoryId)) {
            return false;
        }
        if (Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)) {
            return true;
        }
        int required = resolveRankingConfirmationRequiredCount(table, categoryId);
        return required <= 0 || resolveRankingConfirmationConfirmedCount(table, categoryId) >= required;
    }

    private boolean isRankingConfirmationReady(RoundTable table) {
        if (RoundTargetMode.MEDALS.name().equals(table.getTargetMode())) {
            List<Long> categoryIds = roundTableCategoryService.listCategoryIds(table);
            return !categoryIds.isEmpty() && categoryIds.stream().allMatch(categoryId -> isRankingConfirmationReady(table, categoryId));
        }
        return isRankingConfirmationReady(table, null);
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

    private int currentResultVersion(RoundTableCategoryState state) {
        return state == null || state.getResultVersion() == null ? 0 : state.getResultVersion();
    }

    private int currentResultVersionFor(RoundTable table, Long categoryId) {
        if (categoryId == null || !RoundTargetMode.MEDALS.name().equals(table.getTargetMode())) {
            return currentResultVersion(table);
        }
        return currentResultVersion(roundTableCategoryService.requireState(table, categoryId));
    }

    private String resolveCategoryName(RoundTable table, Long categoryId) {
        if (categoryId == null) return null;
        return roundQuerySupport.listCategoryNames(table.getCompetitionId()).get(categoryId);
    }

    private String writeRankingDraft(List<RankingResultItemRequest> results) {
        try {
            return objectMapper.writeValueAsString(results == null ? List.of() : results);
        } catch (JsonProcessingException ex) {
            throw new BaseException("参考排序保存失败");
        }
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

    private CompetitionType resolveCompetitionType(Competition competition) {
        return CompetitionType.of(competition == null ? null : competition.getCompetitionType());
    }

    private boolean isFeedbackOnlyCompetition(Long competitionId) {
        return resolveCompetitionType(competitionMapper.selectById(competitionId)) == CompetitionType.FEEDBACK_ONLY;
    }
}
