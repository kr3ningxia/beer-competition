package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableConfirmationMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundStatus;
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
import com.beercompetition.pojo.vo.CompetitionLiveBoardVO;
import com.beercompetition.pojo.vo.CompetitionSponsorVO;
import com.beercompetition.pojo.vo.LiveBoardMetricVO;
import com.beercompetition.pojo.vo.LiveBoardSponsorGroupVO;
import com.beercompetition.pojo.vo.LiveBoardSummaryVO;
import com.beercompetition.pojo.vo.LiveBoardTableVO;
import com.beercompetition.service.CompetitionSponsorService;
import com.beercompetition.service.LiveBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LiveBoardServiceImpl implements LiveBoardService {

    private static final int FULL_PERCENT = 100;
    private static final int FLAG_TRUE = 1;
    private static final int FLAG_FALSE = 0;
    private static final String CONFIRMATION_STATUS_AGREED = "AGREED";
    private static final String DEFAULT_SUBTITLE = "The Chinese Lager Awards";

    private final BeerEntryMapper beerEntryMapper;
    private final CompetitionMapper competitionMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundResultMapper roundResultMapper;
    private final RoundTableConfirmationMapper roundTableConfirmationMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final CompetitionSponsorService competitionSponsorService;

    @Override
    public CompetitionLiveBoardVO getCompetitionLiveBoard(Long competitionId) {
        // 1) 参数规范化与前置校验
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }

        // 2) 查询当前公开看板轮次与聚合数据
        CompetitionRound round = resolveLiveRound(competitionId);
        List<LiveBoardSponsorGroupVO> sponsorGroups = buildSponsorGroups(competitionId);
        if (round == null) {
            return buildEmptyBoard(competition, sponsorGroups);
        }
        LiveBoardData data = loadLiveBoardData(competitionId, round);
        List<TableStats> tableStats = data.tables().stream()
                .map(table -> buildTableStats(round, table, data))
                .toList();

        // 3) 组装并返回公开投屏数据
        return CompetitionLiveBoardVO.builder()
                .competitionId(competition.getId())
                .competitionName(competition.getName())
                .subtitle(DEFAULT_SUBTITLE)
                .roundId(round.getId())
                .roundName(round.getRoundName())
                .roundType(round.getRoundType())
                .roundTypeText(RoundType.RANKING.name().equals(round.getRoundType()) ? "排序轮" : "评分制")
                .roundStatus(round.getStatus())
                .statusText(resolveRoundStatusText(round))
                .refreshedAt(LocalDateTime.now())
                .summary(buildSummary(round, tableStats))
                .metrics(buildMetrics(round, data, tableStats))
                .tables(tableStats.stream().map(TableStats::toVO).toList())
                .sponsorGroups(sponsorGroups)
                .build();
    }

    private CompetitionLiveBoardVO buildEmptyBoard(Competition competition, List<LiveBoardSponsorGroupVO> sponsorGroups) {
        return CompetitionLiveBoardVO.builder()
                .competitionId(competition.getId())
                .competitionName(competition.getName())
                .subtitle(DEFAULT_SUBTITLE)
                .roundName("等待轮次发布")
                .roundTypeText("现场准备")
                .statusText("现场整理中")
                .refreshedAt(LocalDateTime.now())
                .summary(LiveBoardSummaryVO.builder()
                        .eyebrow("现场进程")
                        .done(0)
                        .total(0)
                        .percent(0)
                        .label("轮次发布后显示评审进度")
                        .build())
                .metrics(List.of(
                        buildMetric("entries", "参赛酒款", String.valueOf(countCompetitionEntries(competition.getId())), "款", "neutral"),
                        buildMetric("roundEntries", "本轮酒款", "-", "", "neutral"),
                        buildMetric("personalRate", "个人评分", "-", "", "neutral"),
                        buildMetric("pendingFinalize", "待桌长汇总", "-", "", "neutral"),
                        buildMetric("pendingConfirm", "待同桌确认", "-", "", "neutral"),
                        buildMetric("averageComment", "平均评语", "-", "", "neutral")
                ))
                .tables(List.of())
                .sponsorGroups(sponsorGroups)
                .build();
    }

    private CompetitionRound resolveLiveRound(Long competitionId) {
        List<CompetitionRound> rounds = competitionRoundMapper.selectList(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .ne(CompetitionRound::getStatus, RoundStatus.DRAFT.name())
                .orderByDesc(CompetitionRound::getSortOrder)
                .orderByDesc(CompetitionRound::getId));
        Set<String> activeStatuses = Set.of(
                RoundStatus.PUBLISHED.name(),
                RoundStatus.IN_PROGRESS.name(),
                RoundStatus.SUBMITTED.name()
        );
        return rounds.stream()
                .filter(round -> activeStatuses.contains(round.getStatus()))
                .findFirst()
                .orElseGet(() -> rounds.stream()
                        .filter(round -> RoundStatus.LOCKED.name().equals(round.getStatus()))
                        .findFirst()
                        .orElse(null));
    }

    private LiveBoardData loadLiveBoardData(Long competitionId, CompetitionRound round) {
        List<RoundTable> tables = roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getRoundId, round.getId())
                .orderByAsc(RoundTable::getSortOrder)
                .orderByAsc(RoundTable::getId));
        if (tables.isEmpty()) {
            return new LiveBoardData(List.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), countCompetitionEntries(competitionId));
        }
        Set<Long> tableIds = tables.stream().map(RoundTable::getId).collect(Collectors.toSet());
        List<RoundTableEntry> entries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, round.getId())
                .in(RoundTableEntry::getRoundTableId, tableIds));
        List<RoundTableMember> members = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .in(RoundTableMember::getRoundTableId, tableIds));
        List<ScoreRecord> scores = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getCompetitionId, competitionId)
                .eq(ScoreRecord::getRoundId, round.getId()));
        List<RoundTableConfirmation> confirmations = roundTableConfirmationMapper.selectList(new LambdaQueryWrapper<RoundTableConfirmation>()
                .in(RoundTableConfirmation::getRoundTableId, tableIds));
        List<RoundResult> results = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundId, round.getId()));
        return new LiveBoardData(
                tables,
                entries.stream().collect(Collectors.groupingBy(RoundTableEntry::getRoundTableId)),
                members.stream().collect(Collectors.groupingBy(RoundTableMember::getRoundTableId)),
                scores.stream().collect(Collectors.groupingBy(ScoreRecord::getRoundTableId)),
                confirmations.stream().collect(Collectors.groupingBy(RoundTableConfirmation::getRoundTableId)),
                results.stream().collect(Collectors.groupingBy(RoundResult::getRoundTableId)),
                scores.stream().filter(score -> Objects.equals(score.getFinalFlag(), FLAG_FALSE)).collect(Collectors.groupingBy(ScoreRecord::getRoundTableId)),
                countCompetitionEntries(competitionId)
        );
    }

    private TableStats buildTableStats(CompetitionRound round, RoundTable table, LiveBoardData data) {
        List<RoundTableEntry> entries = data.entriesByTable().getOrDefault(table.getId(), List.of());
        List<RoundTableMember> members = data.membersByTable().getOrDefault(table.getId(), List.of());
        List<ScoreRecord> scores = data.scoresByTable().getOrDefault(table.getId(), List.of());
        List<ScoreRecord> judgeScores = data.judgeScoresByTable().getOrDefault(table.getId(), List.of());
        List<RoundTableConfirmation> confirmations = data.confirmationsByTable().getOrDefault(table.getId(), List.of());
        List<RoundResult> results = data.resultsByTable().getOrDefault(table.getId(), List.of());
        int entryCount = entries.size();
        int personalTotal = entryCount * resolveTaskJudgeCount(table, members);
        int personalDone = countDistinctPersonalScores(judgeScores);
        int captainDone = countDistinctFinalScores(scores);
        int confirmationRequired = resolveConfirmationRequired(round, members);
        int confirmationDone = resolveConfirmationDone(table, members, confirmations, round);
        int rankingDone = results.size();
        int rankingTarget = Math.max(NumberUtils.safeInt(table.getTargetCount()), 0);
        int completionPercent = RoundType.RANKING.name().equals(round.getRoundType())
                ? percent(rankingDone, rankingTarget)
                : percent(captainDone, entryCount);
        String statusText = resolveTableStatus(round, table, entryCount, personalDone, personalTotal,
                captainDone, confirmationDone, confirmationRequired, rankingDone, rankingTarget);
        boolean rankingRound = RoundType.RANKING.name().equals(round.getRoundType());
        return new TableStats(
                table.getId(),
                table.getTableName(),
                personalDone,
                personalTotal,
                captainDone,
                entryCount,
                confirmationDone,
                confirmationRequired,
                completionPercent,
                averageCommentChars(judgeScores),
                statusText,
                resolveTone(statusText),
                rankingRound ? formatProgress(rankingDone, rankingTarget) : formatProgress(personalDone, personalTotal),
                rankingRound ? resolveRankingSubmitText(round, table, rankingDone, rankingTarget) : formatProgress(captainDone, entryCount),
                confirmationRequired > 0 ? formatProgress(confirmationDone, confirmationRequired) : "无需确认"
        );
    }

    private int resolveTaskJudgeCount(RoundTable table, List<RoundTableMember> members) {
        int taskJudges = (int) members.stream()
                .filter(member -> Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE))
                .count();
        if (table.getCaptainJudgeId() != null) {
            boolean captainCounted = members.stream()
                    .anyMatch(member -> Objects.equals(member.getJudgeAccountId(), table.getCaptainJudgeId())
                            && Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE));
            if (!captainCounted) {
                taskJudges++;
            }
        }
        return Math.max(taskJudges, 0);
    }

    private int countDistinctPersonalScores(List<ScoreRecord> scores) {
        return (int) scores.stream()
                .filter(score -> Objects.equals(score.getFinalFlag(), FLAG_FALSE))
                .map(score -> score.getJudgeAccountId() + ":" + score.getBeerEntryId())
                .distinct()
                .count();
    }

    private int countDistinctFinalScores(List<ScoreRecord> scores) {
        return (int) scores.stream()
                .filter(score -> Objects.equals(score.getFinalFlag(), FLAG_TRUE))
                .map(ScoreRecord::getBeerEntryId)
                .distinct()
                .count();
    }

    private int resolveConfirmationRequired(CompetitionRound round, List<RoundTableMember> members) {
        return (int) members.stream()
                .filter(member -> !JudgeRoleType.CAPTAIN.name().equals(member.getRole()))
                .filter(member -> RoundType.RANKING.name().equals(round.getRoundType())
                        || Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE))
                .count();
    }

    private int resolveConfirmationDone(RoundTable table, List<RoundTableMember> members,
                                        List<RoundTableConfirmation> confirmations, CompetitionRound round) {
        Set<Long> requiredJudgeIds = members.stream()
                .filter(member -> !JudgeRoleType.CAPTAIN.name().equals(member.getRole()))
                .filter(member -> RoundType.RANKING.name().equals(round.getRoundType())
                        || Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE))
                .map(RoundTableMember::getJudgeAccountId)
                .collect(Collectors.toSet());
        if (requiredJudgeIds.isEmpty()) {
            return 0;
        }
        int resultVersion = table.getResultVersion() == null ? 0 : table.getResultVersion();
        return (int) confirmations.stream()
                .filter(confirmation -> Objects.equals(confirmation.getResultVersion(), resultVersion))
                .filter(confirmation -> CONFIRMATION_STATUS_AGREED.equals(confirmation.getStatus()))
                .filter(confirmation -> requiredJudgeIds.contains(confirmation.getJudgeAccountId()))
                .count();
    }

    private String resolveTableStatus(CompetitionRound round, RoundTable table, int entryCount,
                                      int personalDone, int personalTotal, int captainDone,
                                      int confirmationDone, int confirmationRequired,
                                      int rankingDone, int rankingTarget) {
        if (RoundStatus.LOCKED.name().equals(round.getStatus()) || RoundStatus.LOCKED.name().equals(table.getStatus())) {
            return "本轮已锁定";
        }
        if (RoundStatus.SUBMITTED.name().equals(round.getStatus()) || RoundStatus.SUBMITTED.name().equals(table.getStatus())) {
            return "本轮待确认";
        }
        if (RoundType.RANKING.name().equals(round.getRoundType())) {
            if (rankingTarget > 0 && rankingDone < rankingTarget) {
                return "评审中";
            }
            if (!Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)
                    && confirmationRequired > 0
                    && confirmationDone < confirmationRequired) {
                return "确认中";
            }
            return "本桌完成";
        }
        if (entryCount <= 0) {
            return "现场整理中";
        }
        if (personalDone < personalTotal) {
            return "评审中";
        }
        if (captainDone < entryCount) {
            return "汇总中";
        }
        if (!Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)
                && confirmationRequired > 0
                && confirmationDone < confirmationRequired) {
            return "确认中";
        }
        return "本桌完成";
    }

    private String resolveTone(String statusText) {
        return switch (statusText) {
            case "本轮已锁定", "本桌完成" -> "success";
            case "本轮待确认", "确认中", "汇总中" -> "warning";
            case "评审中" -> "active";
            default -> "neutral";
        };
    }

    private String resolveRankingSubmitText(CompetitionRound round, RoundTable table, int rankingDone, int rankingTarget) {
        if (RoundStatus.SUBMITTED.name().equals(round.getStatus())
                || RoundStatus.LOCKED.name().equals(round.getStatus())
                || RoundStatus.SUBMITTED.name().equals(table.getStatus())
                || RoundStatus.LOCKED.name().equals(table.getStatus())
                || rankingTarget > 0 && rankingDone >= rankingTarget) {
            return "已提交";
        }
        return "待提交";
    }

    private LiveBoardSummaryVO buildSummary(CompetitionRound round, List<TableStats> tableStats) {
        if (RoundType.RANKING.name().equals(round.getRoundType())) {
            int done = (int) tableStats.stream()
                    .filter(stats -> Set.of("本轮待确认", "本轮已锁定", "本桌完成").contains(stats.statusText()))
                    .count();
            int total = tableStats.size();
            return LiveBoardSummaryVO.builder()
                    .eyebrow("排序进度")
                    .done(done)
                    .total(total)
                    .percent(percent(done, total))
                    .label(total > 0 ? "已提交 " + done + " 桌 · 剩余 " + Math.max(total - done, 0) + " 桌" : "等待排序轮发布")
                    .build();
        }
        int done = tableStats.stream().mapToInt(TableStats::captainDone).sum();
        int total = tableStats.stream().mapToInt(TableStats::entryCount).sum();
        return LiveBoardSummaryVO.builder()
                .eyebrow("桌长汇总")
                .done(done)
                .total(total)
                .percent(percent(done, total))
                .label(total > 0 ? "已汇总 " + done + " 款 · 剩余 " + Math.max(total - done, 0) + " 款" : "等待首轮酒款发布")
                .build();
    }

    private List<LiveBoardMetricVO> buildMetrics(CompetitionRound round, LiveBoardData data, List<TableStats> tableStats) {
        boolean rankingRound = RoundType.RANKING.name().equals(round.getRoundType());
        int roundEntries = tableStats.stream().mapToInt(TableStats::entryCount).sum();
        int personalDone = tableStats.stream().mapToInt(TableStats::personalDone).sum();
        int personalTotal = tableStats.stream().mapToInt(TableStats::personalTotal).sum();
        int rankingSubmittedTables = (int) tableStats.stream()
                .filter(stats -> Set.of("本轮待确认", "本轮已锁定", "本桌完成").contains(stats.statusText()))
                .count();
        int pendingFinalize = !rankingRound
                ? tableStats.stream().mapToInt(stats -> Math.max(stats.entryCount() - stats.captainDone(), 0)).sum()
                : Math.max(tableStats.size() - rankingSubmittedTables, 0);
        int pendingConfirm = (int) tableStats.stream()
                .filter(stats -> stats.confirmationRequired() > 0 && stats.confirmationDone() < stats.confirmationRequired())
                .count();
        int averageComment = averageInt(data.judgeScoresByTable().values().stream()
                .flatMap(List::stream)
                .map(ScoreRecord::getCommentCharCount)
                .filter(Objects::nonNull)
                .toList());
        return List.of(
                buildMetric("entries", "参赛酒款", String.valueOf(data.competitionEntryCount()), "款", "neutral"),
                buildMetric("roundEntries", rankingRound ? "本轮候选" : "本轮酒款", String.valueOf(roundEntries), rankingRound ? "款" : "款", "neutral"),
                buildMetric("personalRate", rankingRound ? "排序提交" : "个人评分", rankingRound ? percent(rankingSubmittedTables, tableStats.size()) + "%" : percent(personalDone, personalTotal) + "%", "", resolveProgressTone(rankingRound ? rankingSubmittedTables : personalDone, rankingRound ? tableStats.size() : personalTotal)),
                buildMetric("pendingFinalize", rankingRound ? "待提交桌次" : "待桌长汇总", String.valueOf(pendingFinalize), rankingRound ? "桌" : "款", pendingFinalize > 0 ? "warning" : "success"),
                buildMetric("pendingConfirm", "待同桌确认", String.valueOf(pendingConfirm), "桌", pendingConfirm > 0 ? "warning" : "success"),
                buildMetric("averageComment", "平均评语", averageComment > 0 ? String.valueOf(averageComment) : "-", averageComment > 0 ? "字" : "", "neutral")
        );
    }

    private String resolveProgressTone(int done, int total) {
        return total > 0 && done >= total ? "success" : "gold";
    }

    private LiveBoardMetricVO buildMetric(String key, String label, String value, String unit, String tone) {
        return LiveBoardMetricVO.builder()
                .key(key)
                .label(label)
                .value(value)
                .unit(unit)
                .tone(tone)
                .build();
    }

    private String resolveRoundStatusText(CompetitionRound round) {
        if (RoundStatus.LOCKED.name().equals(round.getStatus())) {
            return "本轮已锁定";
        }
        if (RoundStatus.SUBMITTED.name().equals(round.getStatus())) {
            return "本轮待确认";
        }
        if (RoundStatus.IN_PROGRESS.name().equals(round.getStatus())) {
            return "评审中";
        }
        if (RoundStatus.PUBLISHED.name().equals(round.getStatus())) {
            return "评审中";
        }
        return "现场整理中";
    }

    private List<LiveBoardSponsorGroupVO> buildSponsorGroups(Long competitionId) {
        List<CompetitionSponsorVO> enabledSponsors = competitionSponsorService.listSponsors(competitionId).stream()
                .filter(sponsor -> Boolean.TRUE.equals(sponsor.getEnabled()))
                .sorted(Comparator.comparing(CompetitionSponsorVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(CompetitionSponsorVO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        Map<String, List<CompetitionSponsorVO>> sponsorsByTier = new LinkedHashMap<>();
        enabledSponsors.forEach(sponsor -> sponsorsByTier
                .computeIfAbsent(sponsor.getTierLabel(), key -> new ArrayList<>())
                .add(sponsor));
        return sponsorsByTier.entrySet().stream()
                .map(entry -> LiveBoardSponsorGroupVO.builder()
                        .tierLabel(entry.getKey())
                        .featured(entry.getValue().stream().anyMatch(sponsor -> Boolean.TRUE.equals(sponsor.getFeatured())))
                        .sponsors(entry.getValue())
                        .build())
                .toList();
    }

    private int countCompetitionEntries(Long competitionId) {
        return Math.toIntExact(beerEntryMapper.selectCount(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .ne(BeerEntry::getStatus, EntryStatus.CANCELED.name())));
    }

    private int averageCommentChars(List<ScoreRecord> scores) {
        return averageInt(scores.stream()
                .map(ScoreRecord::getCommentCharCount)
                .filter(Objects::nonNull)
                .toList());
    }

    private int averageInt(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private int percent(int done, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(FULL_PERCENT, done * FULL_PERCENT / total));
    }

    private static String formatProgress(int done, int total) {
        return total > 0 ? done + "/" + total : "-";
    }

    private record LiveBoardData(List<RoundTable> tables,
                                 Map<Long, List<RoundTableEntry>> entriesByTable,
                                 Map<Long, List<RoundTableMember>> membersByTable,
                                 Map<Long, List<ScoreRecord>> scoresByTable,
                                 Map<Long, List<RoundTableConfirmation>> confirmationsByTable,
                                 Map<Long, List<RoundResult>> resultsByTable,
                                 Map<Long, List<ScoreRecord>> judgeScoresByTable,
                                 int competitionEntryCount) {
    }

    private record TableStats(Long id,
                              String displayName,
                              int personalDone,
                              int personalTotal,
                              int captainDone,
                              int entryCount,
                              int confirmationDone,
                              int confirmationRequired,
                              int completionPercent,
                              int averageCommentChars,
                              String statusText,
                              String tone,
                              String personalProgressText,
                              String captainProgressText,
                              String confirmationProgressText) {

        private LiveBoardTableVO toVO() {
            return LiveBoardTableVO.builder()
                    .id(id)
                    .displayName(displayName)
                    .personalProgress(personalProgressText)
                    .captainProgress(captainProgressText)
                    .confirmationProgress(confirmationProgressText)
                    .completionPercent(completionPercent)
                    .averageCommentChars(averageCommentChars)
                    .averageCommentText(averageCommentChars > 0 ? averageCommentChars + "字" : "统计中")
                    .statusText(statusText)
                    .tone(tone)
                    .build();
        }
    }

    private static final class NumberUtils {
        private NumberUtils() {
        }

        private static int safeInt(Integer value) {
            return value == null ? 0 : value;
        }
    }
}
