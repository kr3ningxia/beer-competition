package com.beercompetition.competition.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.competition.FeedbackCommentEditPolicy;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.AdminFeedbackCaptainOpinionVO;
import com.beercompetition.pojo.vo.AdminFeedbackJudgeScoreVO;
import com.beercompetition.pojo.vo.AdminFeedbackReviewEntryVO;
import com.beercompetition.pojo.vo.AdminFeedbackReviewPageVO;
import com.beercompetition.service.EntryScanLabelService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import com.beercompetition.competition.query.CompetitionFeedbackQueryService;

/**
 * 组装反馈审核视图，并保持厂牌信息对评审流程匿名。
 */
@Service
@RequiredArgsConstructor
public class CompetitionFeedbackQueryServiceImpl implements CompetitionFeedbackQueryService {

    private static final BigDecimal SCORE_FORM_TOTAL = BigDecimal.valueOf(50);

    private static final int FLAG_FALSE = 0;

    private static final int FLAG_TRUE = 1;

    private static final String FEEDBACK_STATUS_PUBLISHABLE = "publishable";

    private static final String FEEDBACK_STATUS_AWAITING_CAPTAIN = "awaiting_captain";

    private static final String FEEDBACK_STATUS_COMMENT_MISSING = "comment_missing";

    private static final String FEEDBACK_STATUS_COMMENT_SHORT = "comment_short";

    private static final String JUDGE_ANOMALY_NOT_SUBMITTED = "not_submitted";

    private static final String JUDGE_ANOMALY_COMMENT_MISSING = "comment_missing";

    private static final String JUDGE_ANOMALY_COMMENT_SHORT = "comment_short";

    private static final int DEFAULT_MIN_COMMENT_LENGTH = 0;

    private final CompetitionMapper competitionMapper;

    private final CompetitionAccessService competitionAccessService;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final JudgeAccountMapper judgeAccountMapper;

    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMapper roundTableMapper;

    private final RoundTableEntryMapper roundTableEntryMapper;

    private final RoundTableMemberMapper roundTableMemberMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final ObjectMapper objectMapper;

    @Override
    public List<AdminFeedbackReviewEntryVO> getFeedbackReviewEntries(Long competitionId) {
        return buildFeedbackReviewPage(competitionId, null, null).records();
    }

    @Override
    public AdminFeedbackReviewPageVO getFeedbackReviewPage(Long competitionId, Integer page, Integer pageSize) {
        FeedbackPageData pageData = buildFeedbackReviewPage(competitionId, page, pageSize);
        return AdminFeedbackReviewPageVO.builder()
                .total(pageData.total())
                .records(pageData.records())
                .tableNames(pageData.tableNames())
                .categoryNames(pageData.categoryNames())
                .build();
    }

    private FeedbackPageData buildFeedbackReviewPage(Long competitionId, Integer page, Integer pageSize) {
        // 1) 校验比赛并定位第一轮评分制轮次
        competitionAccessService.requireCompetitionAccess(competitionId);
        Competition competition = getCompetitionOrThrow(competitionId);
        boolean feedbackEditable = isFeedbackCommentEditable(competition);
        CompetitionRound firstScoreRound = competitionRoundMapper.selectOne(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .eq(CompetitionRound::getRoundNo, 1)
                .eq(CompetitionRound::getRoundType, RoundType.SCORE.name())
                .orderByAsc(CompetitionRound::getSortOrder)
                .orderByAsc(CompetitionRound::getId)
                .last("LIMIT 1"));
        if (firstScoreRound == null) {
            return new FeedbackPageData(0, List.of(), List.of(), List.of());
        }

        // 2) 查询第一轮桌次、酒款、评审、标签和评分记录
        List<RoundTable> tables = roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getRoundId, firstScoreRound.getId())
                .orderByAsc(RoundTable::getSortOrder)
                .orderByAsc(RoundTable::getId));
        if (tables.isEmpty()) {
            return new FeedbackPageData(0, List.of(), List.of(), List.of());
        }
        List<Long> tableIds = tables.stream().map(RoundTable::getId).toList();
        List<RoundTableEntry> roundEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, firstScoreRound.getId())
                .orderByAsc(RoundTableEntry::getRoundTableId)
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
        if (roundEntries.isEmpty()) {
            return new FeedbackPageData(0, List.of(), List.of(), List.of());
        }
        int total = roundEntries.size();
        List<RoundTableEntry> pageEntries = roundEntries;
        if (page != null || pageSize != null) {
            int currentPage = page == null || page < 1 ? 1 : page;
            int currentPageSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
            int fromIndex = Math.min((currentPage - 1) * currentPageSize, total);
            int toIndex = Math.min(fromIndex + currentPageSize, total);
            pageEntries = roundEntries.subList(fromIndex, toIndex);
        }
        Set<Long> beerEntryIds = pageEntries.stream().map(RoundTableEntry::getBeerEntryId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<BeerEntry> entries = beerEntryMapper.selectBatchIds(beerEntryIds);
        Map<Long, BeerEntry> entryById = entries.stream()
                .collect(Collectors.toMap(BeerEntry::getId, Function.identity(), (left, right) -> left));
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(new ArrayList<>(beerEntryIds));
        Map<Long, String> categoryNameById = loadCategoryNames(competitionId);
        List<RoundTableMember> members = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .in(RoundTableMember::getRoundTableId, tableIds));
        Map<Long, List<RoundTableMember>> membersByTable = members.stream()
                .collect(Collectors.groupingBy(RoundTableMember::getRoundTableId));
        Map<Long, RoundTable> tableById = tables.stream()
                .collect(Collectors.toMap(RoundTable::getId, Function.identity(), (left, right) -> left));
        Map<Long, JudgeAccount> judgeById = loadReviewJudges(tables, members);
        Map<String, ScoreRecord> personalScoreByJudgeEntry = loadPersonalScores(competitionId, beerEntryIds);
        Map<Long, ScoreRecord> finalScoreByEntry = loadFinalScores(competitionId, beerEntryIds);
        Map<String, Integer> minCommentLengthByRole = loadMinCommentLengthByRole(competitionId);

        // 3) 组装发布前反馈复核列表
        List<AdminFeedbackReviewEntryVO> records = pageEntries.stream()
                .map(roundEntry -> buildFeedbackReviewEntry(firstScoreRound, roundEntry, tableById, entryById,
                        labelByEntryId, categoryNameById, membersByTable, judgeById, personalScoreByJudgeEntry,
                        finalScoreByEntry, minCommentLengthByRole, feedbackEditable))
                .filter(Objects::nonNull)
                .toList();
        return new FeedbackPageData(total, records,
                tables.stream().map(RoundTable::getTableName).filter(Objects::nonNull).distinct().toList(),
                categoryNameById.values().stream().filter(Objects::nonNull).distinct().sorted().toList());
    }

    private record FeedbackPageData(long total, List<AdminFeedbackReviewEntryVO> records,
                                    List<String> tableNames, List<String> categoryNames) {
    }

    private AdminFeedbackReviewEntryVO buildFeedbackReviewEntry(CompetitionRound round,
                                                                RoundTableEntry roundEntry,
                                                                Map<Long, RoundTable> tableById,
                                                                Map<Long, BeerEntry> entryById,
                                                                Map<Long, EntryScanLabel> labelByEntryId,
                                                                Map<Long, String> categoryNameById,
                                                                Map<Long, List<RoundTableMember>> membersByTable,
                                                                Map<Long, JudgeAccount> judgeById,
                                                                Map<String, ScoreRecord> personalScoreByJudgeEntry,
                                                                Map<Long, ScoreRecord> finalScoreByEntry,
                                                                Map<String, Integer> minCommentLengthByRole,
                                                                boolean feedbackEditable) {
        RoundTable table = tableById.get(roundEntry.getRoundTableId());
        BeerEntry entry = entryById.get(roundEntry.getBeerEntryId());
        if (table == null || entry == null) {
            return null;
        }
        List<RoundTableMember> reviewMembers = normalizeReviewMembers(table, membersByTable.getOrDefault(table.getId(), List.of()))
                .stream()
                .filter(member -> Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE))
                .toList();
        ScoreRecord finalScore = finalScoreByEntry.get(entry.getId());
        List<AdminFeedbackJudgeScoreVO> judges = reviewMembers.stream()
                .map(member -> buildFeedbackJudgeScore(member, entry, judgeById, personalScoreByJudgeEntry, minCommentLengthByRole, feedbackEditable))
                .toList();
        EntryScanLabel label = labelByEntryId.get(entry.getId());
        int personalSubmitted = (int) judges.stream().filter(item -> Boolean.TRUE.equals(item.getScored())).count();
        return AdminFeedbackReviewEntryVO.builder()
                .roundId(round.getId())
                .roundTableId(table.getId())
                .beerEntryId(entry.getId())
                .beerUuid(entry.getUuid())
                .entryName(firstText(entry.getName(), firstText(label == null ? null : label.getShortCode(), entry.getUuid())))
                .labelCode(firstText(label == null ? null : label.getLabelCode(), entry.getUuid()))
                .shortCode(label == null ? "" : label.getShortCode())
                .roundName(round.getRoundName())
                .tableName(table.getTableName())
                .categoryName(categoryNameById.getOrDefault(entry.getCategoryId(), "未分组"))
                .style(entry.getStyle())
                .personalSubmitted(personalSubmitted)
                .personalTotal(reviewMembers.size())
                .captainSubmitted(finalScore != null)
                .consensusScore(finalScore == null ? null : firstScore(finalScore.getConsensusScore(), finalScore.getTotalScore()))
                .advanced(finalScore != null && Objects.equals(finalScore.getAdvancedFlag(), FLAG_TRUE))
                .status(resolveFeedbackStatus(finalScore, judges, minCommentLengthByRole))
                .captainOpinion(buildCaptainOpinion(table, finalScore, judgeById, minCommentLengthByRole, feedbackEditable))
                .judges(judges)
                .build();
    }

    private AdminFeedbackJudgeScoreVO buildFeedbackJudgeScore(RoundTableMember member,
                                                              BeerEntry entry,
                                                              Map<Long, JudgeAccount> judgeById,
                                                              Map<String, ScoreRecord> personalScoreByJudgeEntry,
                                                              Map<String, Integer> minCommentLengthByRole,
                                                              boolean feedbackEditable) {
        JudgeAccount judge = judgeById.get(member.getJudgeAccountId());
        ScoreRecord score = personalScoreByJudgeEntry.get(scoreKey(member.getJudgeAccountId(), entry.getId()));
        List<DimensionRequest> dimensions = score == null ? List.of() : readDimensions(score.getDimensionsJson());
        String role = member.getRole();
        return AdminFeedbackJudgeScoreVO.builder()
                .scoreRecordId(score == null ? null : score.getId())
                .judgePublicId(judge == null ? "" : judge.getPublicId())
                .judgeName(judge == null ? "未知评审" : judge.getName())
                .role(role)
                .roleLabel(feedbackRoleLabel(role))
                .scored(score != null)
                .editable(score != null && feedbackEditable)
                .totalScore(score == null ? null : score.getTotalScore())
                .maxTotal(dimensions.isEmpty() ? SCORE_FORM_TOTAL : getScoreTotal(dimensions))
                .submittedAt(score == null ? null : firstTime(score.getUpdateTime(), score.getCreateTime()))
                .updatedAt(score == null ? null : firstTime(score.getUpdateTime(), score.getCreateTime()))
                .durationSeconds(score == null ? null : score.getDurationSeconds())
                .commentCharCount(score == null ? 0 : resolveFeedbackCommentCharCount(score))
                .minCommentLength(minCommentLengthByRole.getOrDefault(role, DEFAULT_MIN_COMMENT_LENGTH))
                .dimensions(dimensions)
                .comments(score == null ? "" : score.getComments())
                .anomaly(score == null ? JUDGE_ANOMALY_NOT_SUBMITTED : resolveCommentAnomaly(score, role, minCommentLengthByRole))
                .build();
    }

    private AdminFeedbackCaptainOpinionVO buildCaptainOpinion(RoundTable table,
                                                              ScoreRecord finalScore,
                                                              Map<Long, JudgeAccount> judgeById,
                                                              Map<String, Integer> minCommentLengthByRole,
                                                              boolean feedbackEditable) {
        JudgeAccount captain = judgeById.get(table.getCaptainJudgeId());
        if (finalScore == null) {
            return AdminFeedbackCaptainOpinionVO.builder()
                    .scoreRecordId(null)
                    .submitted(false)
                    .editable(false)
                    .captainName(captain == null ? "未知桌长" : captain.getName())
                    .maxConsensus(SCORE_FORM_TOTAL)
                    .advanced(false)
                    .build();
        }
        List<DimensionRequest> dimensions = readDimensions(finalScore.getDimensionsJson());
        return AdminFeedbackCaptainOpinionVO.builder()
                .scoreRecordId(finalScore.getId())
                .submitted(true)
                .editable(feedbackEditable)
                .captainName(captain == null ? "未知桌长" : captain.getName())
                .consensusScore(firstScore(finalScore.getConsensusScore(), finalScore.getTotalScore()))
                .maxConsensus(dimensions.isEmpty() ? SCORE_FORM_TOTAL : getScoreTotal(dimensions))
                .advanced(Objects.equals(finalScore.getAdvancedFlag(), FLAG_TRUE))
                .comments(finalScore.getComments())
                .submittedAt(firstTime(finalScore.getUpdateTime(), finalScore.getCreateTime()))
                .updatedAt(firstTime(finalScore.getUpdateTime(), finalScore.getCreateTime()))
                .commentCharCount(resolveFeedbackCommentCharCount(finalScore))
                .minCommentLength(minCommentLengthByRole.getOrDefault(JudgeRoleType.CAPTAIN.name(), DEFAULT_MIN_COMMENT_LENGTH))
                .build();
    }

    private boolean isFeedbackCommentEditable(Competition competition) {
        return competition != null && FeedbackCommentEditPolicy.isEditable(competition.getStatus());
    }

    private String resolveFeedbackStatus(ScoreRecord finalScore,
                                         List<AdminFeedbackJudgeScoreVO> judges,
                                         Map<String, Integer> minCommentLengthByRole) {
        if (finalScore == null) {
            return FEEDBACK_STATUS_AWAITING_CAPTAIN;
        }
        String captainAnomaly = resolveCommentAnomaly(finalScore, JudgeRoleType.CAPTAIN.name(), minCommentLengthByRole);
        if (JUDGE_ANOMALY_COMMENT_MISSING.equals(captainAnomaly)
                || judges.stream().anyMatch(judge -> JUDGE_ANOMALY_COMMENT_MISSING.equals(judge.getAnomaly()))) {
            return FEEDBACK_STATUS_COMMENT_MISSING;
        }
        if (JUDGE_ANOMALY_COMMENT_SHORT.equals(captainAnomaly)
                || judges.stream().anyMatch(judge -> JUDGE_ANOMALY_COMMENT_SHORT.equals(judge.getAnomaly()))) {
            return FEEDBACK_STATUS_COMMENT_SHORT;
        }
        return FEEDBACK_STATUS_PUBLISHABLE;
    }

    private String resolveCommentAnomaly(ScoreRecord record, String role, Map<String, Integer> minCommentLengthByRole) {
        int effectiveChars = resolveFeedbackCommentCharCount(record);
        if (effectiveChars == 0) {
            return JUDGE_ANOMALY_COMMENT_MISSING;
        }
        int minLength = minCommentLengthByRole.getOrDefault(role, DEFAULT_MIN_COMMENT_LENGTH);
        if (minLength > 0 && effectiveChars < minLength) {
            return JUDGE_ANOMALY_COMMENT_SHORT;
        }
        return null;
    }

    private int resolveFeedbackCommentCharCount(ScoreRecord record) {
        if (record == null) {
            return 0;
        }
        Integer storedCount = record.getCommentCharCount();
        if (storedCount != null && storedCount > 0) {
            return storedCount;
        }
        return countEffectiveChars(effectiveFeedbackText(record));
    }

    private String effectiveFeedbackText(ScoreRecord record) {
        if (record == null) {
            return "";
        }
        String dimensionText = readDimensions(record.getDimensionsJson()).stream()
                .map(DimensionRequest::getNote)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        return StringUtils.hasText(dimensionText) ? dimensionText : record.getComments();
    }

    private List<RoundTableMember> normalizeReviewMembers(RoundTable table, List<RoundTableMember> members) {
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
                .filter(member -> member.getJudgeAccountId() != null)
                .sorted(Comparator.comparing(RoundTableMember::getRole, Comparator.nullsLast(String::compareTo))
                        .thenComparing(RoundTableMember::getJudgeAccountId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private Map<Long, JudgeAccount> loadReviewJudges(List<RoundTable> tables, List<RoundTableMember> members) {
        Set<Long> judgeIds = new LinkedHashSet<>();
        tables.stream().map(RoundTable::getCaptainJudgeId).filter(Objects::nonNull).forEach(judgeIds::add);
        members.stream().map(RoundTableMember::getJudgeAccountId).filter(Objects::nonNull).forEach(judgeIds::add);
        return loadJudgeAccounts(judgeIds);
    }

    private Map<String, ScoreRecord> loadPersonalScores(Long competitionId, Set<Long> beerEntryIds) {
        if (beerEntryIds.isEmpty()) {
            return Map.of();
        }
        return scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, competitionId)
                        .in(ScoreRecord::getBeerEntryId, beerEntryIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_FALSE))
                .stream()
                .collect(Collectors.toMap(
                        record -> scoreKey(record.getJudgeAccountId(), record.getBeerEntryId()),
                        Function.identity(),
                        this::preferLatestScore));
    }

    private Map<Long, ScoreRecord> loadFinalScores(Long competitionId, Set<Long> beerEntryIds) {
        if (beerEntryIds.isEmpty()) {
            return Map.of();
        }
        return scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, competitionId)
                        .in(ScoreRecord::getBeerEntryId, beerEntryIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), this::preferLatestScore));
    }

    private Map<Long, String> loadCategoryNames(Long competitionId) {
        return competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId))
                .stream()
                .collect(Collectors.toMap(CompetitionCategory::getId, CompetitionCategory::getName, (left, right) -> left));
    }

    private Map<String, Integer> loadMinCommentLengthByRole(Long competitionId) {
        return competitionScoreConfigMapper.selectList(new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId))
                .stream()
                .collect(Collectors.toMap(CompetitionScoreConfig::getJudgeRoleType,
                        config -> resolveMinCommentLength(config.getMinCommentLength()),
                        (left, right) -> left));
    }

    private ScoreRecord preferLatestScore(ScoreRecord left, ScoreRecord right) {
        LocalDateTime leftTime = firstTime(left.getUpdateTime(), left.getCreateTime());
        LocalDateTime rightTime = firstTime(right.getUpdateTime(), right.getCreateTime());
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return leftTime.isAfter(rightTime) ? left : right;
    }

    private String scoreKey(Long judgeAccountId, Long beerEntryId) {
        return judgeAccountId + ":" + beerEntryId;
    }

    private BigDecimal firstScore(BigDecimal primary, BigDecimal fallback) {
        return primary == null ? fallback : primary;
    }

    private LocalDateTime firstTime(LocalDateTime primary, LocalDateTime fallback) {
        return primary == null ? fallback : primary;
    }

    private int countEffectiveChars(String text) {
        return StringUtils.hasText(text) ? text.replaceAll("\\s+", "").length() : 0;
    }

    private String feedbackRoleLabel(String role) {
        if (JudgeRoleType.CAPTAIN.name().equals(role)) {
            return "桌长个人评分";
        }
        if (JudgeRoleType.PROFESSIONAL.name().equals(role)) {
            return "专业评审";
        }
        if (JudgeRoleType.CROSS.name().equals(role)) {
            return "跨界评审";
        }
        return "评审";
    }

    private Map<Long, JudgeAccount> loadJudgeAccounts(Set<Long> judgeIds) {
        if (judgeIds.isEmpty()) {
            return Map.of();
        }
        return judgeAccountMapper.selectBatchIds(judgeIds).stream()
                .collect(Collectors.toMap(JudgeAccount::getId, Function.identity(), (left, right) -> left));
    }

    private String shortCode(BeerEntry entry, Map<Long, EntryScanLabel> labelByEntryId) {
        EntryScanLabel label = labelByEntryId.get(entry.getId());
        return label == null ? "" : firstText(label.getShortCode(), "");
    }

    private String categoryName(BeerEntry entry, Map<Long, CompetitionCategory> categoryById) {
        CompetitionCategory category = categoryById.get(entry.getCategoryId());
        return category == null ? "" : firstText(category.getName(), "");
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : (fallback == null ? "" : fallback);
    }

    private BigDecimal getScoreTotal(List<DimensionRequest> dimensions) {
        return dimensions.stream()
                .map(DimensionRequest::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Competition getCompetitionOrThrow(Long id) {
        Competition competition = competitionMapper.selectById(id);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private Integer resolveMinCommentLength(Integer minCommentLength) {
        return minCommentLength == null ? DEFAULT_MIN_COMMENT_LENGTH : minCommentLength;
    }

    private List<DimensionRequest> readDimensions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<DimensionRequest>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BaseException("解析评分维度失败");
        }
    }
}
