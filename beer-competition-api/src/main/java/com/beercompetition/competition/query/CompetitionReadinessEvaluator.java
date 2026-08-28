package com.beercompetition.competition.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.AwardResultStatus;
import com.beercompetition.pojo.enums.AwardType;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RefundApprovalMode;
import com.beercompetition.pojo.enums.OrganizerType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.CompetitionAlertVO;
import com.beercompetition.pojo.vo.CompetitionCheckVO;
import com.beercompetition.pojo.vo.CompetitionConfigNameVO;
import com.beercompetition.pojo.vo.CompetitionPrimaryActionVO;
import com.beercompetition.pojo.vo.CompetitionStageCheckVO;
import com.beercompetition.pojo.vo.EntryFieldConfigVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.JudgeTableVO;
import com.beercompetition.pojo.vo.AwardResultVO;
import com.beercompetition.pojo.vo.ResultSetupVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.service.AwardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * 计算赛事阶段检查、可编辑范围和下一步动作，不写入业务数据。
 */
@Service
@RequiredArgsConstructor
public class CompetitionReadinessEvaluator {

    private static final BigDecimal SCORE_FORM_TOTAL = BigDecimal.valueOf(50);

    private static final String CHECK_DONE = "done";

    private static final String CHECK_PENDING = "pending";

    private static final String CHECK_LOCKED = "locked";

    private static final String CHECK_INVALID = "invalid";

    private static final String GROUP_CURRENT = "current";

    private static final String GROUP_REQUIRED = "required";

    private static final String GROUP_FUTURE = "future";

    private static final String GROUP_LOCKED = "locked";

    private static final String GROUP_DATA_ISSUE = "data_issue";

    private static final int CROSS_MIN_DIMENSIONS = 2;

    private static final int CROSS_MAX_DIMENSIONS = 3;

    private static final List<String> PROFESSIONAL_DIMENSION_LABELS = List.of("香气", "外观", "味道", "口感", "整体印象");

    private static final List<BigDecimal> PROFESSIONAL_DIMENSION_MAX_SCORES = List.of(
                BigDecimal.valueOf(12),
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(10));

    private final ScoreRecordMapper scoreRecordMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMapper roundTableMapper;

    private final RoundResultMapper roundResultMapper;

    private final RoundTableEntryMapper roundTableEntryMapper;

    private final AwardService awardService;

    private final OrganizerMapper organizerMapper;

    private void validateEarlyBirdConfig(BigDecimal earlyBirdFee,
                                         LocalDateTime earlyBirdDeadline,
                                         BigDecimal entryFee,
                                         LocalDateTime registrationStart,
                                         LocalDateTime registrationDeadline) {
        if ((earlyBirdFee == null) != (earlyBirdDeadline == null)) {
            throw new BaseException("早鸟价和早鸟价截止时间需要同时填写");
        }
        if (earlyBirdFee == null) {
            return;
        }
        if (entryFee == null) {
            throw new BaseException("报名费不能为空");
        }
        if (earlyBirdFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new BaseException("早鸟价不能小于 0");
        }
        if (earlyBirdFee.compareTo(entryFee) > 0) {
            throw new BaseException("早鸟价不能高于报名费");
        }
        if (registrationStart != null && !earlyBirdDeadline.isAfter(registrationStart)) {
            throw new BaseException("早鸟价截止时间必须晚于报名开始时间");
        }
        if (registrationDeadline != null && earlyBirdDeadline.isAfter(registrationDeadline)) {
            throw new BaseException("早鸟价截止时间不能晚于报名截止时间");
        }
    }

    public List<CompetitionCheckVO> buildListChecks(Competition competition,
                                                      boolean hasCategories,
                                                      boolean hasJudgeTables,
                                                      List<ScoreConfigVO> scoreConfigs,
                                                      EntrySummaryVO entriesSummary) {
        List<CompetitionCheckVO> checks = new ArrayList<>();
        checks.add(check("baseInfo", "基础信息", isBaseInfoReady(competition), "名称、日期、简介、报名时间和费用需要完整"));
        checks.add(check("categories", "投递组别", hasCategories, "至少配置 1 个投递组别"));
        checks.add(check("styleLibrary", "基础风格库", StringUtils.hasText(competition.getStyleLibraryVersion()), "请选择基础风格库"));
        checks.add(check("entryFields", "补充字段", true, "未配置补充字段"));
        checks.add(check("judgeTables", "评审桌", hasJudgeTables, "至少配置 1 张评审桌"));
        checks.add(check("scoreForms", "评分表", isScoreFormsReady(scoreConfigs), "跨界、专业、桌长三类评分表都必须为 50 分"));
        checks.add(check("storedEntries", "酒款入库", entriesSummary.getStored() > 0, "至少确认 1 款样品入库"));
        boolean published = Set.of(CompetitionStatus.PUBLISHED, CompetitionStatus.ARCHIVED).contains(parseStatus(competition));
        checks.add(check("resultSetup", "结果发布", published,
                resolveCompetitionType(competition) == CompetitionType.FEEDBACK_ONLY
                        ? "首轮锁定后可发布诊断结果"
                        : "奖项确认后才能发布结果"));

        Map<String, Boolean> editableScopes = buildEditableScopes(competition);
        if (!editableScopes.get("entryStructure")) {
            markLocked(checks, Set.of("categories", "styleLibrary", "entryFields"), "报名已开放，报名结构已锁定");
        }
        if (!editableScopes.get("judgeConfig")) {
            markLocked(checks, Set.of("judgeTables", "scoreForms"), "评审已开始，评审配置已锁定");
        }
        return checks;
    }

    public ResultSetupVO buildResultSetup(Competition competition) {
        CompetitionStatus status = parseStatus(competition);
        boolean published = status == CompetitionStatus.PUBLISHED || status == CompetitionStatus.ARCHIVED;
        if (resolveCompetitionType(competition) == CompetitionType.FEEDBACK_ONLY) {
            CompetitionRound lockedScoreRound = findLockedScoreRound(competition.getId());
            boolean firstRoundLocked = lockedScoreRound != null;
            int entryCount = 0;
            int finalizedCount = 0;
            int evaluatedCount = 0;
            if (lockedScoreRound != null) {
                List<RoundTableEntry> entries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundId, lockedScoreRound.getId()));
                Set<Long> entryIds = entries.stream()
                        .map(RoundTableEntry::getBeerEntryId)
                        .collect(Collectors.toSet());
                entryCount = entryIds.size();
                if (!entryIds.isEmpty()) {
                    finalizedCount = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                                    .eq(ScoreRecord::getCompetitionId, competition.getId())
                                    .eq(ScoreRecord::getFinalFlag, 1)
                                    .in(ScoreRecord::getBeerEntryId, entryIds))
                            .stream()
                            .map(ScoreRecord::getBeerEntryId)
                            .collect(Collectors.toSet())
                            .size();
                    evaluatedCount = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                                    .eq(RoundResult::getCompetitionId, competition.getId())
                                    .eq(RoundResult::getRoundId, lockedScoreRound.getId())
                                    .eq(RoundResult::getResultType, RoundResultType.EVALUATED.name())
                                    .in(RoundResult::getBeerEntryId, entryIds))
                            .stream()
                            .map(RoundResult::getBeerEntryId)
                            .collect(Collectors.toSet())
                            .size();
                }
            }
            boolean feedbackReady = entryCount > 0 && finalizedCount >= entryCount && evaluatedCount >= entryCount;
            return ResultSetupVO.builder()
                    .awardsReady(firstRoundLocked || published)
                    .published(published)
                    .championReady(published)
                    .medalsReady(published)
                    .terminalRoundLocked(firstRoundLocked || published)
                    .canPublishResults(!published && status == CompetitionStatus.JUDGING && firstRoundLocked && feedbackReady)
                    .feedbackEntryCount(entryCount)
                    .feedbackFinalizedCount(finalizedCount)
                    .feedbackEvaluatedCount(evaluatedCount)
                    .build();
        }
        List<AwardResultVO> awardResults = awardService.listAwardResults(competition.getId());
        boolean medalsReady = areMedalAwardsReady(competition.getId(), awardResults);
        boolean championReady = awardResults.stream().anyMatch(result -> Boolean.TRUE.equals(result.getChampion())
                && (AwardResultStatus.CONFIRMED.name().equals(result.getStatus()) || AwardResultStatus.PUBLISHED.name().equals(result.getStatus())));
        boolean terminalRoundLocked = hasLockedTerminalRound(competition.getId());
        boolean canPublishResults = published || (status == CompetitionStatus.RESULT_CONFIRMING
                && terminalRoundLocked && medalsReady && championReady);
        return ResultSetupVO.builder()
                .awardsReady((medalsReady && championReady) || published)
                .published(published)
                .championReady(championReady || published)
                .medalsReady(medalsReady || published)
                .terminalRoundLocked(terminalRoundLocked || published)
                .canPublishResults(canPublishResults)
                .build();
    }

    private boolean hasLockedTerminalRound(Long competitionId) {
        return competitionRoundMapper.selectList(new LambdaQueryWrapper<CompetitionRound>()
                        .eq(CompetitionRound::getCompetitionId, competitionId)
                        .eq(CompetitionRound::getRoundType, RoundType.RANKING.name())
                        .eq(CompetitionRound::getStatus, RoundStatus.LOCKED.name()))
                .stream()
                .anyMatch(round -> roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                                .eq(RoundTable::getRoundId, round.getId()))
                        .stream()
                        .anyMatch(table -> RoundTargetMode.CHAMPION.name().equals(table.getTargetMode())));
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

    private boolean areMedalAwardsReady(Long competitionId, List<AwardResultVO> awardResults) {
        return awardResults.stream()
                .filter(result -> AwardType.MEDAL.name().equals(result.getAwardType()))
                .filter(result -> result.getCategoryId() != null)
                .filter(result -> AwardResultStatus.CONFIRMED.name().equals(result.getStatus())
                        || AwardResultStatus.PUBLISHED.name().equals(result.getStatus()))
                .anyMatch(result -> result.getRankNo() != null && result.getRankNo() >= 1 && result.getRankNo() <= 3);
    }

    public List<CompetitionCheckVO> buildChecks(Competition competition,
                                                 List<CompetitionConfigNameVO> categories,
                                                 List<CompetitionConfigNameVO> styles,
                                                 List<EntryFieldConfigVO> entryFields,
                                                 List<JudgeTableVO> judgeTables,
                                                 List<ScoreConfigVO> scoreConfigs,
                                                 EntrySummaryVO entriesSummary,
                                                 ResultSetupVO resultSetup) {
        List<CompetitionCheckVO> checks = new ArrayList<>();
        checks.add(check("baseInfo", "基础信息", isBaseInfoReady(competition), "名称、日期、简介、报名时间和费用需要完整"));
        checks.add(check("categories", "投递组别", !categories.isEmpty(), "至少配置 1 个投递组别"));
        checks.add(check("styleLibrary", "基础风格库", StringUtils.hasText(competition.getStyleLibraryVersion()), "请选择基础风格库"));
        checks.add(check("entryFields", "补充字段", true, entryFields.isEmpty() ? "未配置补充字段" : "已配置补充字段"));
        checks.add(check("judgeTables", "评审桌", !judgeTables.isEmpty(), "至少配置 1 张评审桌"));
        checks.add(check("scoreForms", "评分表", isScoreFormsReady(scoreConfigs), "跨界、专业、桌长三类评分表都必须为 50 分"));
        checks.add(check("storedEntries", "酒款入库", entriesSummary.getStored() > 0, "至少确认 1 款样品入库"));
        checks.add(check("resultSetup", "结果发布", Boolean.TRUE.equals(resultSetup.getPublished()),
                resolveCompetitionType(competition) == CompetitionType.FEEDBACK_ONLY ? "首轮锁定后可发布诊断结果" : "奖项确认后才能发布结果"));

        if (!buildEditableScopes(competition).get("entryStructure")) {
            markLocked(checks, Set.of("categories", "styleLibrary", "entryFields"), "报名已开放，报名结构已锁定");
        }
        if (!buildEditableScopes(competition).get("judgeConfig")) {
            markLocked(checks, Set.of("judgeTables", "scoreForms"), "评审已开始，评审配置已锁定");
        }
        return checks;
    }

    private CompetitionCheckVO check(String key, String label, boolean ready, String message) {
        return CompetitionCheckVO.builder()
                .key(key)
                .label(label)
                .state(ready ? CHECK_DONE : CHECK_PENDING)
                .message(ready ? "已完成" : message)
                .build();
    }

    private void markLocked(List<CompetitionCheckVO> checks, Set<String> keys, String message) {
        checks.stream()
                .filter(check -> keys.contains(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .forEach(check -> {
                    check.setState(CHECK_LOCKED);
                    check.setMessage(message);
                });
    }

    public List<String> buildDataIntegrityIssues(Competition competition, List<CompetitionCheckVO> checks) {
        if (parseStatus(competition) == CompetitionStatus.DRAFT) {
            return List.of();
        }
        return checks.stream()
                .filter(check -> isBlockingCheck(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .map(check -> "比赛已进入" + resolveStageLabel(parseStatus(competition)) + "，但" + check.getLabel() + "未完成")
                .toList();
    }

    public List<CompetitionStageCheckVO> buildStageChecks(Competition competition,
                                                           List<CompetitionCheckVO> checks,
                                                           List<String> dataIntegrityIssues) {
        CompetitionStatus status = parseStatus(competition);
        boolean hasDataIssue = !dataIntegrityIssues.isEmpty();
        return checks.stream()
                .map(check -> CompetitionStageCheckVO.builder()
                        .key(check.getKey())
                        .label(check.getLabel())
                        .state(hasDataIssue && isBlockingCheck(check.getKey()) && !CHECK_DONE.equals(check.getState())
                                ? CHECK_INVALID
                                : check.getState())
                        .group(resolveCheckGroup(status, check, hasDataIssue))
                        .message(resolveStageCheckMessage(status, check, hasDataIssue))
                        .targetTab(resolveCheckTargetTab(check.getKey()))
                        .build())
                .toList();
    }

    public List<CompetitionAlertVO> buildAlerts(List<CompetitionCheckVO> checks,
                                                 EntrySummaryVO entriesSummary,
                                                 List<String> dataIntegrityIssues) {
        if (!dataIntegrityIssues.isEmpty()) {
            return List.of(CompetitionAlertVO.builder()
                    .level("danger")
                    .text("当前比赛数据不一致：比赛已开放报名，但缺少开放报名必需配置。请管理员修正数据。")
                    .build());
        }
        List<CompetitionAlertVO> alerts = checks.stream()
                .filter(check -> isBlockingCheck(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .map(check -> CompetitionAlertVO.builder()
                        .level(CHECK_LOCKED.equals(check.getState()) ? "warning" : "danger")
                        .text(resolveAlertText(check))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
        if (entriesSummary.getPendingPayment() > 0) {
            alerts.add(CompetitionAlertVO.builder()
                    .level("warning")
                    .text("还有 " + entriesSummary.getPendingPayment() + " 款酒等待支付；请进入参赛酒款跟进支付。")
                    .build());
        }
        return alerts;
    }

    public CompetitionPrimaryActionVO buildPrimaryAction(Competition competition,
                                                          List<CompetitionStageCheckVO> stageChecks,
                                                          List<String> dataIntegrityIssues) {
        if (!dataIntegrityIssues.isEmpty()) {
            return CompetitionPrimaryActionVO.builder()
                    .text("查看数据异常")
                    .targetTab("overview")
                    .enabled(true)
                    .build();
        }
        CompetitionStatus status = parseStatus(competition);
        return switch (status) {
            case DRAFT -> {
                boolean canOpen = stageChecks.stream()
                        .filter(check -> GROUP_REQUIRED.equals(check.getGroup()))
                        .allMatch(check -> CHECK_DONE.equals(check.getState()));
                yield CompetitionPrimaryActionVO.builder()
                        .text(canOpen ? "开放报名" : "补齐开放报名配置")
                        .targetTab(canOpen ? "overview" : resolveFirstPendingTarget(stageChecks))
                        .enabled(true)
                        .build();
            }
            case REGISTRATION_OPEN -> CompetitionPrimaryActionVO.builder()
                    .text("截止报名")
                    .targetTab("entries")
                    .enabled(true)
                    .build();
            case REGISTRATION_CLOSED -> CompetitionPrimaryActionVO.builder()
                    .text("完成样品入库核对，进入评审准备中")
                    .targetTab("judges")
                    .enabled(true)
                    .build();
            case JUDGING_PREP -> CompetitionPrimaryActionVO.builder()
                    .text("创建首轮")
                    .targetTab("judges")
                    .enabled(true)
                    .build();
            case JUDGING -> CompetitionPrimaryActionVO.builder()
                    .text("查看现场进度")
                    .targetTab("progress")
                    .enabled(true)
                    .build();
            case RESULT_CONFIRMING -> CompetitionPrimaryActionVO.builder()
                    .text("确认结果发布")
                    .targetTab("results")
                    .enabled(true)
                    .build();
            case PUBLISHED, ARCHIVED -> CompetitionPrimaryActionVO.builder()
                    .text("查看结果与导出")
                    .targetTab("results")
                    .enabled(true)
                    .build();
        };
    }

    private String resolveCheckGroup(CompetitionStatus status, CompetitionCheckVO check, boolean hasDataIssue) {
        if (hasDataIssue && isBlockingCheck(check.getKey()) && !CHECK_DONE.equals(check.getState())) {
            return GROUP_DATA_ISSUE;
        }
        if (CHECK_LOCKED.equals(check.getState())) {
            return GROUP_LOCKED;
        }
        if (status == CompetitionStatus.DRAFT && isBlockingCheck(check.getKey())) {
            return GROUP_REQUIRED;
        }
        if ((status == CompetitionStatus.REGISTRATION_OPEN || status == CompetitionStatus.REGISTRATION_CLOSED)
                && Set.of("judgeTables", "scoreForms").contains(check.getKey())) {
            return GROUP_CURRENT;
        }
        if (status == CompetitionStatus.JUDGING_PREP
                && Set.of("judgeTables", "scoreForms", "storedEntries").contains(check.getKey())) {
            return GROUP_CURRENT;
        }
        if (status == CompetitionStatus.JUDGING && "storedEntries".equals(check.getKey())) {
            return GROUP_CURRENT;
        }
        if (status == CompetitionStatus.RESULT_CONFIRMING && "resultSetup".equals(check.getKey())) {
            return GROUP_CURRENT;
        }
        return GROUP_FUTURE;
    }

    private String resolveStageCheckMessage(CompetitionStatus status, CompetitionCheckVO check, boolean hasDataIssue) {
        if (hasDataIssue && isBlockingCheck(check.getKey()) && !CHECK_DONE.equals(check.getState())) {
            return "系统数据需修正：" + check.getLabel() + "缺失，请管理员处理。";
        }
        if (CHECK_DONE.equals(check.getState())) {
            return "已完成";
        }
        if (CHECK_LOCKED.equals(check.getState())) {
            return check.getMessage() + "。若需调整，请先关闭报名或由管理员修正数据。";
        }
        if (status == CompetitionStatus.DRAFT && isBlockingCheck(check.getKey())) {
            return check.getMessage() + "，完成后才能开放报名。";
        }
        if ("scoreForms".equals(check.getKey())) {
            return "评分表未完成，会影响评审开始；请进入评分表配置。";
        }
        return check.getMessage();
    }

    private String resolveAlertText(CompetitionCheckVO check) {
        String target = switch (check.getKey()) {
            case "categories", "styleLibrary", "entryFields" -> "报名配置";
            case "judgeTables" -> "评审配置";
            case "scoreForms" -> "评分表";
            default -> "概览";
        };
        return check.getLabel() + "未完成，会影响比赛推进；请进入" + target + "处理。";
    }

    private String resolveCheckTargetTab(String key) {
        return switch (key) {
            case "categories", "styleLibrary", "entryFields" -> "entryConfig";
            case "judgeTables" -> "judges";
            case "scoreForms" -> "score";
            case "storedEntries" -> "entries";
            case "resultSetup" -> "results";
            default -> "overview";
        };
    }

    private String resolveFirstPendingTarget(List<CompetitionStageCheckVO> stageChecks) {
        return stageChecks.stream()
                .filter(check -> GROUP_REQUIRED.equals(check.getGroup()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .findFirst()
                .map(CompetitionStageCheckVO::getTargetTab)
                .orElse("overview");
    }

    public Map<String, Boolean> buildEditableScopes(Competition competition) {
        CompetitionStatus status = parseStatus(competition);
        Map<String, Boolean> scopes = new LinkedHashMap<>();
        boolean draft = status == CompetitionStatus.DRAFT;
        boolean registrationStage = status == CompetitionStatus.REGISTRATION_OPEN || status == CompetitionStatus.REGISTRATION_CLOSED;
        boolean judgePrep = status == CompetitionStatus.JUDGING_PREP;
        boolean judgeConfig = draft || registrationStage || judgePrep;
        scopes.put("baseInfo", draft || registrationStage);
        scopes.put("basePrice", draft || registrationStage);
        scopes.put("description", true);
        scopes.put("entryStructure", draft);
        scopes.put("categories", draft);
        scopes.put("styleLibrary", draft || status == CompetitionStatus.REGISTRATION_OPEN);
        scopes.put("styles", draft || status == CompetitionStatus.REGISTRATION_OPEN);
        scopes.put("entryFields", draft || status == CompetitionStatus.REGISTRATION_OPEN);
        scopes.put("refundPolicy", isRefundPolicyEditable(competition, LocalDateTime.now()));
        scopes.put("judgeConfig", judgeConfig);
        scopes.put("judgeTables", judgeConfig);
        scopes.put("scoreConfigs", judgeConfig);
        scopes.put("resultConfig", status == CompetitionStatus.RESULT_CONFIRMING);
        return scopes;
    }

    public RefundApprovalMode resolveRefundApprovalMode(Competition competition) {
        if (competition != null) {
            var organizer = organizerMapper.selectById(competition.getOrganizerId());
            if (organizer != null && OrganizerType.TENANT.name().equals(organizer.getOrganizerType())) {
                return RefundApprovalMode.MANUAL_REVIEW;
            }
        }
        return RefundApprovalMode.of(competition == null ? null : competition.getRefundApprovalMode());
    }

    public boolean isRefundPolicyEditable(Competition competition, LocalDateTime now) {
        if (competition == null || parseStatus(competition) == CompetitionStatus.ARCHIVED
                || competition.getRegistrationDeadline() == null) {
            return false;
        }
        return !now.isAfter(competition.getRegistrationDeadline());
    }

    private boolean isScoreFormsReady(List<ScoreConfigVO> scoreConfigs) {
        if (scoreConfigs.size() != JudgeRoleType.values().length) {
            return false;
        }
        Set<String> roles = scoreConfigs.stream()
                .map(ScoreConfigVO::getJudgeRoleType)
                .collect(Collectors.toSet());
        if (!roles.containsAll(EnumSet.allOf(JudgeRoleType.class).stream().map(Enum::name).toList())) {
            return false;
        }
        return scoreConfigs.stream().allMatch(config -> getScoreTotal(config.getDimensions()).compareTo(SCORE_FORM_TOTAL) == 0
                && isScoreConfigShapeReady(config));
    }

    private boolean isScoreConfigShapeReady(ScoreConfigVO config) {
        if (JudgeRoleType.CROSS.name().equals(config.getJudgeRoleType())) {
            return config.getDimensions().size() >= CROSS_MIN_DIMENSIONS && config.getDimensions().size() <= CROSS_MAX_DIMENSIONS;
        }
        if (JudgeRoleType.CAPTAIN.name().equals(config.getJudgeRoleType())) {
            return config.getDimensions().size() == 1;
        }
        if (JudgeRoleType.PROFESSIONAL.name().equals(config.getJudgeRoleType())) {
            if (config.getDimensions().size() != PROFESSIONAL_DIMENSION_LABELS.size()) {
                return false;
            }
            for (int index = 0; index < PROFESSIONAL_DIMENSION_LABELS.size(); index++) {
                DimensionRequest dimension = config.getDimensions().get(index);
                if (!PROFESSIONAL_DIMENSION_LABELS.get(index).equals(dimension.getLabel())
                        || dimension.getMaxScore() == null
                        || dimension.getMaxScore().compareTo(PROFESSIONAL_DIMENSION_MAX_SCORES.get(index)) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private BigDecimal getScoreTotal(List<DimensionRequest> dimensions) {
        return dimensions.stream()
                .map(DimensionRequest::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public CompetitionStatus parseStatus(Competition competition) {
        try {
            return CompetitionStatus.valueOf(competition.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BaseException("比赛状态不合法：" + competition.getStatus());
        }
    }

    public CompetitionType resolveCompetitionType(Competition competition) {
        return CompetitionType.of(competition == null ? null : competition.getCompetitionType());
    }

    private boolean isBaseInfoReady(Competition competition) {
        return StringUtils.hasText(competition.getName())
                && StringUtils.hasText(competition.getCode())
                && competition.getCompetitionDate() != null
                && competition.getRegistrationStart() != null
                && competition.getRegistrationDeadline() != null
                && competition.getRegistrationDeadline().isAfter(competition.getRegistrationStart())
                && competition.getEntryFee() != null
                && competition.getEntryFee().compareTo(BigDecimal.ZERO) >= 0
                && StringUtils.hasText(competition.getDescription())
                && isEarlyBirdConfigReady(competition);
    }

    private boolean isEarlyBirdConfigReady(Competition competition) {
        try {
            validateEarlyBirdConfig(competition.getEarlyBirdFee(), competition.getEarlyBirdDeadline(),
                    competition.getEntryFee(), competition.getRegistrationStart(), competition.getRegistrationDeadline());
            return true;
        } catch (BaseException ex) {
            return false;
        }
    }

    private boolean isBlockingCheck(String key) {
        return Set.of("baseInfo", "categories", "styleLibrary").contains(key);
    }

    public int countDoneChecks(List<CompetitionCheckVO> checks) {
        return (int) checks.stream()
                .filter(check -> CHECK_DONE.equals(check.getState()))
                .count();
    }

    public String resolveNextAction(CompetitionStatus status) {
        return switch (status) {
            case DRAFT -> "完善配置并开放报名";
            case REGISTRATION_OPEN -> "查看报名酒款";
            case REGISTRATION_CLOSED -> "检查入库情况";
            case JUDGING_PREP -> "校准评审配置";
            case JUDGING -> "查看现场进度";
            case RESULT_CONFIRMING -> "确认结果发布";
            case PUBLISHED, ARCHIVED -> "查看归档数据";
        };
    }

    public String resolveStageLabel(CompetitionStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case REGISTRATION_OPEN -> "报名中";
            case REGISTRATION_CLOSED -> "报名截止";
            case JUDGING_PREP -> "评审准备";
            case JUDGING -> "评审中";
            case RESULT_CONFIRMING -> "结果确认";
            case PUBLISHED -> "已发布";
            case ARCHIVED -> "已归档";
        };
    }
}
