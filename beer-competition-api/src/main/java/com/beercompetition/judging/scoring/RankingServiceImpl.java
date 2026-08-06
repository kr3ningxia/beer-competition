package com.beercompetition.judging.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundJudgeRankingDraftMapper;
import com.beercompetition.mapper.RoundTableConfirmationMapper;
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
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.RankingConfirmationSlotVO;
import com.beercompetition.pojo.vo.RankingConfirmationVO;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import com.beercompetition.service.impl.round.RoundValidationPolicy;
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

    private final RoundJudgeRankingDraftMapper roundJudgeRankingDraftMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final ObjectMapper objectMapper;

    private final RoundQuerySupport roundQuerySupport;

    private final RoundValidationPolicy roundValidationPolicy;

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
        if (RoundStatus.SUBMITTED.name().equals(table.getStatus()) || RoundStatus.LOCKED.name().equals(table.getStatus())) {
            throw new BaseException("本桌排序已提交或已锁定，不能调整");
        }
        assertCompetitionNotArchived(table.getCompetitionId());
        requireRankingCaptainMember(roundTableId, judgeId);
        roundValidationPolicy.validateRankingSubmit(table, request);

        // 2) 写入待确认排序结果和桌内酒款状态
        int nextVersion = currentResultVersion(table) + 1;
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

        // 3) 开启新的同桌确认版本
        table.setStatus(RoundStatus.IN_PROGRESS.name());
        table.setResultVersion(nextVersion);
        table.setConfirmationOverrideFlag(FLAG_FALSE);
        table.setConfirmationOverrideReason(null);
        table.setConfirmationOverrideBy(null);
        table.setConfirmationOverrideTime(null);
        roundTableMapper.updateById(table);
        if (RoundStatus.SUBMITTED.name().equals(round.getStatus())) {
            round.setStatus(RoundStatus.IN_PROGRESS.name());
            round.setSubmittedTime(null);
            competitionRoundMapper.updateById(round);
        }
        autoSubmitRoundTableIfReady(roundTableMapper.selectById(roundTableId), competitionRoundMapper.selectById(round.getId()));
    }

    @Override
    public RankingConfirmationVO getRankingConfirmation(Long roundTableId) {
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
        return buildRankingConfirmation(table, member);
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
        if (JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
            throw new ForbiddenException("当前账号不需要确认本桌排序");
        }
        roundValidationPolicy.validateRankingRoundTableReady(table);

        // 2) 写入当前版本确认记录
        int version = currentResultVersion(table);
        validateConfirmationVersion(request, version);
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

        // 3) 当前版本确认完成后自动提交本桌排序
        autoSubmitRoundTableIfReady(roundTableMapper.selectById(roundTableId), round);
        return buildRankingConfirmation(roundTableMapper.selectById(roundTableId), member);
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
        requireRoundTableMember(roundTableId, judgeId);
        roundValidationPolicy.validateRankingDraft(table, request.getResults());

        // 2) 用覆盖写方式保存草稿，保证同一评审同一桌只有一份参考排序
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

    private RankingConfirmationVO buildRankingConfirmation(RoundTable table, RoundTableMember currentMember) {
        List<RoundResult> results = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundTableId, table.getId())
                .orderByAsc(RoundResult::getRankNo));
        Set<Long> entryIds = results.stream().map(RoundResult::getBeerEntryId).collect(Collectors.toSet());
        Map<Long, BeerEntry> entryById = roundQuerySupport.loadEntries(entryIds);
        Map<Long, String> categoryNameById = roundQuerySupport.listCategoryNames(table.getCompetitionId());
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entryIds);
        Long judgeId = currentMember == null ? null : currentMember.getJudgeAccountId();
        int version = currentResultVersion(table);
        boolean mineConfirmed = judgeId != null && roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getJudgeAccountId, judgeId)
                .eq(RoundTableConfirmation::getResultVersion, version)) > 0;
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
                .tableName(table.getTableName())
                .status(table.getStatus())
                .targetMode(table.getTargetMode())
                .resultVersion(version)
                .confirmedCount(resolveRankingConfirmationConfirmedCount(table))
                .requiredCount(resolveRankingConfirmationRequiredCount(table))
                .mineConfirmed(mineConfirmed)
                .readyForConfirmation(roundValidationPolicy.isRankingRoundTableReady(table))
                .readyForFinalSubmit(isRankingConfirmationReady(table))
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
        if (advancedCount != targetCount) {
            throw new BaseException("晋级数量必须等于目标数量 " + targetCount);
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

    private int resolveRankingConfirmationRequiredCount(RoundTable table) {
        return Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, table.getId())
                .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name())));
    }

    private int resolveRankingConfirmationConfirmedCount(RoundTable table) {
        Set<Long> requiredJudgeIds = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                        .eq(RoundTableMember::getRoundTableId, table.getId())
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

    private void validateRankingRoundTableConfirmations(RoundTable table) {
        if (Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)) {
            return;
        }
        int required = resolveRankingConfirmationRequiredCount(table);
        if (required <= 0) {
            return;
        }
        int confirmed = resolveRankingConfirmationConfirmedCount(table);
        if (confirmed < required) {
            throw new BaseException("同桌评审确认未完成，暂不能提交本桌排序");
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
            roundValidationPolicy.validateRankingRoundTableReady(table);
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
    }

    private boolean isRankingConfirmationReady(RoundTable table) {
        if (!roundValidationPolicy.isRankingRoundTableReady(table)) {
            return false;
        }
        if (Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)) {
            return true;
        }
        int required = resolveRankingConfirmationRequiredCount(table);
        return required <= 0 || resolveRankingConfirmationConfirmedCount(table) >= required;
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
