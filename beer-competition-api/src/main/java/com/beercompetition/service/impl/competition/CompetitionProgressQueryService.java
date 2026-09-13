package com.beercompetition.service.impl.competition;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.vo.CompetitionEntryStatsVO;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.ProgressSummaryVO;
import com.beercompetition.pojo.vo.ScoreProgressStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通过聚合查询生成比赛报名和评审进度，避免读取完整酒款及评分记录。
 */
@Service
@RequiredArgsConstructor
public class CompetitionProgressQueryService {

    private static final int FLAG_TRUE = 1;
    private static final int DEFAULT_MIN_COMMENT_LENGTH = 0;
    private static final String LEGACY_ENTRY_PUBLISHED_STATUS = "PUBLISHED";

    private final BeerEntryMapper beerEntryMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundResultMapper roundResultMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final ScoreRecordMapper scoreRecordMapper;

    public EntrySummaryVO getEntrySummary(Long competitionId) {
        CompetitionEntryStatsVO stats = beerEntryMapper.selectCompetitionStats(
                        List.of(competitionId),
                        EntryStatus.PENDING_PAYMENT.name(),
                        EntryStatus.CANCELED.name(),
                        EntryStatus.RESULT_PUBLISHED.name(),
                        LEGACY_ENTRY_PUBLISHED_STATUS,
                        FLAG_TRUE)
                .stream()
                .findFirst()
                .orElse(null);
        if (stats == null) {
            return EntrySummaryVO.builder()
                    .total(0)
                    .pendingPayment(0)
                    .registered(0)
                    .stored(0)
                    .canceled(0)
                    .resultPublished(0)
                    .build();
        }
        int activeEntries = toInt(stats.getRegisteredCount());
        return EntrySummaryVO.builder()
                .total(activeEntries)
                .pendingPayment(toInt(stats.getPendingPaymentCount()))
                .registered(activeEntries)
                .stored(toInt(stats.getStoredCount()))
                .canceled(0)
                .resultPublished(toInt(stats.getResultPublishedCount()))
                .build();
    }

    public ProgressSummaryVO getProgressSummary(Long competitionId, EntrySummaryVO entrySummary) {
        CompetitionRound currentRound = findCurrentRound(competitionId);
        if (currentRound == null) {
            ScoreProgressStatsVO stats = scoreRecordMapper.selectFinalProgressStats(competitionId);
            return baseProgress(stats, entrySummary.getRegistered());
        }
        if (RoundType.RANKING.name().equals(currentRound.getRoundType())) {
            return buildRankingProgress(currentRound);
        }
        return buildScoreProgress(competitionId, currentRound);
    }

    public CompetitionRoundVO getCurrentRoundSummary(Long competitionId) {
        CompetitionRound currentRound = findCurrentRound(competitionId);
        if (currentRound == null) {
            return null;
        }
        return CompetitionRoundVO.builder()
                .id(currentRound.getId())
                .roundNo(currentRound.getRoundNo())
                .name(currentRound.getRoundName())
                .type(currentRound.getRoundType())
                .status(currentRound.getStatus())
                .allocationRevision(currentRound.getAllocationRevision() == null ? 0L : currentRound.getAllocationRevision())
                .sourceRoundId(currentRound.getSourceRoundId())
                .sourceEntryUuids(List.of())
                .sourceLocked(false)
                .candidatesSynced(false)
                .preparationDraft(false)
                .tables(List.of())
                .build();
    }

    private CompetitionRound findCurrentRound(Long competitionId) {
        return competitionRoundMapper.selectOne(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .ne(CompetitionRound::getStatus, RoundStatus.DRAFT.name())
                .orderByDesc(CompetitionRound::getSortOrder)
                .orderByDesc(CompetitionRound::getId)
                .last("LIMIT 1"));
    }

    private ProgressSummaryVO buildRankingProgress(CompetitionRound currentRound) {
        long tableCount = roundTableMapper.selectCount(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getRoundId, currentRound.getId()));
        long submittedTableCount = roundTableMapper.selectCount(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getRoundId, currentRound.getId())
                .in(RoundTable::getStatus, RoundStatus.SUBMITTED.name(), RoundStatus.LOCKED.name()));
        long resultCount = roundResultMapper.selectCount(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundId, currentRound.getId()));
        return ProgressSummaryVO.builder()
                .finalized(toInt(submittedTableCount))
                .total(toInt(tableCount))
                .advanced(toInt(resultCount))
                .commentWarnings(0)
                .averageReviewMinutes(roundResultMapper.selectAverageReviewMinutes(
                        currentRound.getId(), currentRound.getPublishedTime()))
                .averageReviewSeconds(0)
                .averageCommentChars(0)
                .build();
    }

    private ProgressSummaryVO buildScoreProgress(Long competitionId, CompetitionRound currentRound) {
        long total = roundTableEntryMapper.selectCount(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, currentRound.getId()));
        CompetitionScoreConfig captainConfig = competitionScoreConfigMapper.selectOne(
                new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .select(CompetitionScoreConfig::getMinCommentLength)
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId)
                        .eq(CompetitionScoreConfig::getJudgeRoleType, JudgeRoleType.CAPTAIN.name())
                        .last("LIMIT 1"));
        int captainMinCommentLength = captainConfig == null || captainConfig.getMinCommentLength() == null
                ? DEFAULT_MIN_COMMENT_LENGTH
                : captainConfig.getMinCommentLength();
        ScoreProgressStatsVO stats = scoreRecordMapper.selectRoundProgressStats(
                competitionId,
                currentRound.getId(),
                captainMinCommentLength,
                currentRound.getPublishedTime());
        return ProgressSummaryVO.builder()
                .finalized(toInt(stats == null ? null : stats.getFinalizedCount()))
                .total(toInt(total))
                .advanced(toInt(stats == null ? null : stats.getAdvancedCount()))
                .commentWarnings(captainMinCommentLength <= DEFAULT_MIN_COMMENT_LENGTH
                        ? 0
                        : toInt(stats == null ? null : stats.getCommentWarningCount()))
                .averageReviewMinutes(toRoundedInt(stats == null ? null : stats.getAverageReviewMinutes()))
                .averageReviewSeconds(toRoundedInt(stats == null ? null : stats.getAverageReviewSeconds()))
                .averageCommentChars(toRoundedInt(stats == null ? null : stats.getAverageCommentChars()))
                .build();
    }

    private ProgressSummaryVO baseProgress(ScoreProgressStatsVO stats, int total) {
        return ProgressSummaryVO.builder()
                .finalized(toInt(stats == null ? null : stats.getFinalizedCount()))
                .total(total)
                .advanced(toInt(stats == null ? null : stats.getAdvancedCount()))
                .commentWarnings(0)
                .averageReviewMinutes(0)
                .averageReviewSeconds(0)
                .averageCommentChars(0)
                .build();
    }

    private int toInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }

    private int toRoundedInt(Double value) {
        return value == null ? 0 : Math.toIntExact(Math.round(value));
    }
}
