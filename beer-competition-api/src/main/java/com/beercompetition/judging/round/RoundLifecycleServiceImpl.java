package com.beercompetition.judging.round;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.billing.beercoin.BeerCoinSettlementService;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableConfirmationMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundEntryStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableConfirmation;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.service.AwardService;
import com.beercompetition.service.CompetitionJudgePublicProfileService;
import com.beercompetition.service.EmailNotificationService;
import com.beercompetition.judging.assignment.RoundCandidateSyncService;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import com.beercompetition.service.impl.round.RoundValidationPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_TRUE;
import com.beercompetition.judging.round.RoundLifecycleService;

/**
 * 推进轮次和结果状态，并保持反馈赛与奖项赛的原有分支。
 */
@Service
@RequiredArgsConstructor
public class RoundLifecycleServiceImpl implements RoundLifecycleService {

    private final CompetitionMapper competitionMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMapper roundTableMapper;

    private final RoundTableMemberMapper roundTableMemberMapper;

    private final RoundTableEntryMapper roundTableEntryMapper;

    private final RoundResultMapper roundResultMapper;

    private final RoundTableConfirmationMapper roundTableConfirmationMapper;

    private final AwardService awardService;

    private final RoundQuerySupport roundQuerySupport;

    private final RoundValidationPolicy roundValidationPolicy;

    private final RoundCandidateSyncService roundCandidateSyncService;

    private final BeerCoinSettlementService beerCoinSettlementService;

    private final CompetitionJudgePublicProfileService competitionJudgePublicProfileService;

    private final EmailNotificationService emailNotificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishRound(Long competitionId, Long roundId) {
        // 1) 查询轮次并校验发布条件
        Competition competition = roundQuerySupport.requireCompetition(competitionId);
        CompetitionRound round = roundQuerySupport.requireRoundForUpdate(competitionId, roundId);
        if (!RoundStatus.DRAFT.name().equals(round.getStatus())) {
            throw new BaseException("只有草稿轮次可以发布");
        }
        beerCoinSettlementService.requireJudgingSettlementCompleted(competitionId);
        roundValidationPolicy.validateCompetitionStageForRoundPublish(competition, round);
        roundValidationPolicy.validateRoundReady(competition, round);

        // 2) 更新轮次和桌任务状态
        String nextStatus = RoundType.SCORE.name().equals(round.getRoundType()) ? RoundStatus.PUBLISHED.name() : RoundStatus.IN_PROGRESS.name();
        round.setStatus(nextStatus);
        round.setPublishedTime(LocalDateTime.now());
        competitionRoundMapper.updateById(round);
        for (RoundTable table : roundQuerySupport.listRoundTables(roundId)) {
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
        Competition competition = roundQuerySupport.requireCompetition(competitionId);
        if (roundValidationPolicy.parseCompetitionStatus(competition) != CompetitionStatus.JUDGING) {
            throw new BaseException("只有评审中的比赛可以确认首轮完成");
        }
        CompetitionRound round = roundQuerySupport.requireRound(competitionId, roundId);
        if (!RoundType.SCORE.name().equals(round.getRoundType())) {
            throw new BaseException("只有首轮评分制轮次可以执行此操作");
        }
        if (resolveCompetitionType(competition) == CompetitionType.FEEDBACK_ONLY) {
            completeFeedbackOnlyFirstRound(competitionId, roundId, round);
            return;
        }
        List<RoundTableEntry> entries = roundQuerySupport.listRoundEntries(roundId);
        if (entries.isEmpty()) {
            throw new BaseException("首轮没有酒款分配");
        }
        List<RoundTable> tables = roundQuerySupport.listRoundTables(roundId);
        Map<Long, RoundTable> tableById = tables.stream().collect(Collectors.toMap(RoundTable::getId, Function.identity()));
        Map<Long, ScoreRecord> finalScoreByEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, competitionId)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        Map<Long, List<RoundTableEntry>> entriesByTable = entries.stream().collect(Collectors.groupingBy(RoundTableEntry::getRoundTableId));
        for (RoundTable table : tables) {
            if (!RoundStatus.SUBMITTED.name().equals(table.getStatus())) {
                throw new BaseException(table.getTableName() + "本桌结果尚未提交");
            }
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
        roundCandidateSyncService.syncDependentDrafts(round);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockRound(Long competitionId, Long roundId) {
        // 1) 查询轮次并校验锁定状态
        Competition competition = roundQuerySupport.requireCompetition(competitionId);
        if (!roundValidationPolicy.isRankingRoundStage(roundValidationPolicy.parseCompetitionStatus(competition))) {
            throw new BaseException("当前阶段不能锁定排序轮");
        }
        CompetitionRound round = roundQuerySupport.requireRound(competitionId, roundId);
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
        roundCandidateSyncService.syncDependentDrafts(round);
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
        Competition competition = roundQuerySupport.requireCompetition(competitionId);
        if (resolveCompetitionType(competition) == CompetitionType.FEEDBACK_ONLY) {
            publishFeedbackOnlyResults(competitionId, competition);
            return;
        }
        if (roundValidationPolicy.parseCompetitionStatus(competition) != CompetitionStatus.RESULT_CONFIRMING) {
            throw new BaseException("请先完成评审并确认奖项，再发布结果");
        }
        CompetitionRound lastLocked = roundQuerySupport.findLastLockedRound(competitionId);
        if (lastLocked == null || !isTerminalRound(lastLocked)) {
            throw new BaseException("决赛轮结果未锁定，暂不能发布结果");
        }
        awardService.publishAwards(competitionId);
        competitionJudgePublicProfileService.snapshotAtPublication(competitionId);

        // 2) 发布比赛状态
        competition.setStatus(CompetitionStatus.PUBLISHED.name());
        competition.setUpdateTime(LocalDateTime.now());
        competitionMapper.updateById(competition);
        emailNotificationService.generateScheduledNotifications(LocalDateTime.now());
    }

    private void completeFeedbackOnlyFirstRound(Long competitionId, Long roundId, CompetitionRound round) {
        // 1) 校验首轮桌长汇总和同桌确认
        List<RoundTableEntry> entries = roundQuerySupport.listRoundEntries(roundId);
        if (entries.isEmpty()) {
            throw new BaseException("首轮没有酒款分配");
        }
        List<RoundTable> tables = roundQuerySupport.listRoundTables(roundId);
        Map<Long, RoundTable> tableById = tables.stream().collect(Collectors.toMap(RoundTable::getId, Function.identity()));
        Map<Long, ScoreRecord> finalScoreByEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, competitionId)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        Map<Long, List<RoundTableEntry>> entriesByTable = entries.stream().collect(Collectors.groupingBy(RoundTableEntry::getRoundTableId));
        for (RoundTable table : tables) {
            if (!RoundStatus.SUBMITTED.name().equals(table.getStatus())) {
                throw new BaseException(table.getTableName() + "本桌诊断尚未提交");
            }
            List<RoundTableEntry> tableEntries = entriesByTable.getOrDefault(table.getId(), List.of());
            List<Long> missing = tableEntries.stream()
                    .map(RoundTableEntry::getBeerEntryId)
                    .filter(entryId -> !finalScoreByEntry.containsKey(entryId))
                    .toList();
            if (!missing.isEmpty()) {
                throw new BaseException(table.getTableName() + "还有酒款未完成桌长汇总");
            }
            validateScoreRoundTableConfirmations(table);
        }

        // 2) 写入中性诊断结果
        roundResultMapper.delete(new LambdaQueryWrapper<RoundResult>().eq(RoundResult::getRoundId, roundId));
        for (RoundTableEntry entry : entries) {
            ScoreRecord finalScore = finalScoreByEntry.get(entry.getBeerEntryId());
            RoundTable table = tableById.get(entry.getRoundTableId());
            entry.setStatus(RoundEntryStatus.ASSIGNED.name());
            roundTableEntryMapper.updateById(entry);
            roundResultMapper.insert(RoundResult.builder()
                    .competitionId(competitionId)
                    .roundId(roundId)
                    .roundTableId(entry.getRoundTableId())
                    .beerEntryId(entry.getBeerEntryId())
                    .resultType(RoundResultType.EVALUATED.name())
                    .rankNo(null)
                    .slotLabel("已完成诊断")
                    .submittedBy(finalScore.getJudgeAccountId())
                    .submittedTime(LocalDateTime.now())
                    .lockedFlag(FLAG_TRUE)
                    .build());
        }

        // 3) 锁定首轮
        lockRoundAndTables(round);
    }

    private void publishFeedbackOnlyResults(Long competitionId, Competition competition) {
        // 1) 校验诊断发布条件
        if (roundValidationPolicy.parseCompetitionStatus(competition) != CompetitionStatus.JUDGING) {
            throw new BaseException("请先完成首轮评审，再发布诊断结果");
        }
        CompetitionRound scoreRound = findLockedScoreRound(competitionId);
        if (scoreRound == null) {
            throw new BaseException("首轮评审尚未锁定，暂不能发布诊断结果");
        }
        List<RoundTableEntry> entries = roundQuerySupport.listRoundEntries(scoreRound.getId());
        if (entries.isEmpty()) {
            throw new BaseException("首轮没有酒款分配");
        }
        Set<Long> entryIds = entries.stream().map(RoundTableEntry::getBeerEntryId).collect(Collectors.toSet());
        Set<Long> finalScoreEntryIds = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, competitionId)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE)
                        .in(ScoreRecord::getBeerEntryId, entryIds))
                .stream()
                .map(ScoreRecord::getBeerEntryId)
                .collect(Collectors.toSet());
        if (!finalScoreEntryIds.containsAll(entryIds)) {
            throw new BaseException("还有酒款未完成桌长汇总，暂不能发布诊断结果");
        }
        long evaluatedCount = roundResultMapper.selectCount(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getCompetitionId, competitionId)
                .eq(RoundResult::getRoundId, scoreRound.getId())
                .eq(RoundResult::getResultType, RoundResultType.EVALUATED.name())
                .in(RoundResult::getBeerEntryId, entryIds));
        if (evaluatedCount < entryIds.size()) {
            throw new BaseException("诊断结果尚未生成，请先完成首轮评审");
        }

        // 2) 生成公开评委快照并发布比赛与酒款结果
        competitionJudgePublicProfileService.snapshotAtPublication(competitionId);
        competition.setStatus(CompetitionStatus.PUBLISHED.name());
        competition.setUpdateTime(LocalDateTime.now());
        competitionMapper.updateById(competition);
        beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                        .eq(BeerEntry::getCompetitionId, competitionId)
                        .ne(BeerEntry::getStatus, EntryStatus.CANCELED.name()))
                .forEach(entry -> {
                    entry.setStatus(EntryStatus.RESULT_PUBLISHED.name());
                    beerEntryMapper.updateById(entry);
                });
        emailNotificationService.generateScheduledNotifications(LocalDateTime.now());
    }

    private CompetitionRound findLockedScoreRound(Long competitionId) {
        return competitionRoundMapper.selectList(new LambdaQueryWrapper<CompetitionRound>()
                        .eq(CompetitionRound::getCompetitionId, competitionId)
                        .eq(CompetitionRound::getRoundType, RoundType.SCORE.name())
                        .eq(CompetitionRound::getStatus, RoundStatus.LOCKED.name())
                        .orderByDesc(CompetitionRound::getRoundNo)
                        .orderByDesc(CompetitionRound::getId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void lockRoundAndTables(CompetitionRound round) {
        round.setStatus(RoundStatus.LOCKED.name());
        round.setLockedTime(LocalDateTime.now());
        competitionRoundMapper.updateById(round);
        for (RoundTable table : roundQuerySupport.listRoundTables(round.getId())) {
            table.setStatus(RoundStatus.LOCKED.name());
            roundTableMapper.updateById(table);
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

    private int currentResultVersion(RoundTable table) {
        return table.getResultVersion() == null ? 0 : table.getResultVersion();
    }

    private boolean isAwardRound(CompetitionRound round) {
        List<RoundTable> tables = roundQuerySupport.listRoundTables(round.getId());
        return RoundType.RANKING.name().equals(round.getRoundType())
                && !tables.isEmpty()
                && tables.stream().allMatch(table -> RoundTargetMode.MEDALS.name().equals(table.getTargetMode())
                || RoundTargetMode.CHAMPION.name().equals(table.getTargetMode()));
    }

    private boolean isTerminalRound(CompetitionRound round) {
        List<RoundTable> tables = roundQuerySupport.listRoundTables(round.getId());
        return RoundType.RANKING.name().equals(round.getRoundType())
                && !tables.isEmpty()
                && tables.stream().allMatch(table -> RoundTargetMode.CHAMPION.name().equals(table.getTargetMode()));
    }

    private CompetitionType resolveCompetitionType(Competition competition) {
        return CompetitionType.of(competition == null ? null : competition.getCompetitionType());
    }
}
