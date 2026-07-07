package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.AwardResultMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.CompetitionBaseInfoUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.CompetitionReopenRegistrationRequest;
import com.beercompetition.pojo.dto.CompetitionReturnToSampleCheckRequest;
import com.beercompetition.pojo.dto.CompetitionStyleLibraryUpdateRequest;
import com.beercompetition.pojo.dto.ConfigNameBatchUpdateRequest;
import com.beercompetition.pojo.dto.ConfigNameItemRequest;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.EntryFieldBatchUpdateRequest;
import com.beercompetition.pojo.dto.EntryFieldItemRequest;
import com.beercompetition.pojo.dto.JudgeTableBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeTableItemRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.dto.ScoreConfigItemRequest;
import com.beercompetition.pojo.enums.CompetitionDeliveryMethod;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.AwardResultStatus;
import com.beercompetition.pojo.enums.AwardType;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.LogisticsVisibility;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.AwardResult;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.AdminFeedbackCaptainOpinionVO;
import com.beercompetition.pojo.vo.AdminFeedbackJudgeScoreVO;
import com.beercompetition.pojo.vo.AdminFeedbackReviewEntryVO;
import com.beercompetition.pojo.vo.CompetitionAlertVO;
import com.beercompetition.pojo.vo.CompetitionAnalyticsVO;
import com.beercompetition.pojo.vo.CompetitionCheckVO;
import com.beercompetition.pojo.vo.CompetitionConfigNameVO;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.CompetitionLogisticsVO;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.pojo.vo.CompetitionPrimaryActionVO;
import com.beercompetition.pojo.vo.CompetitionStageCheckVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.EntryFieldConfigVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.JudgeAssignmentVO;
import com.beercompetition.pojo.vo.JudgeTableVO;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.pojo.vo.PortalHomeVO;
import com.beercompetition.pojo.vo.ProgressSummaryVO;
import com.beercompetition.pojo.vo.AwardResultVO;
import com.beercompetition.pojo.vo.AwardRuleVO;
import com.beercompetition.pojo.vo.ResultDraftVO;
import com.beercompetition.pojo.vo.ResultSetupVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.pojo.vo.StyleItemVO;
import com.beercompetition.service.AwardService;
import com.beercompetition.service.CompetitionService;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.RoundService;
import com.beercompetition.service.StyleLibraryService;
import com.beercompetition.common.context.BaseContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private static final BigDecimal SCORE_FORM_TOTAL = BigDecimal.valueOf(50);
    private static final String CHECK_DONE = "done";
    private static final String CHECK_PENDING = "pending";
    private static final String CHECK_LOCKED = "locked";
    private static final String CHECK_INVALID = "invalid";
    private static final int FLAG_FALSE = 0;
    private static final int FLAG_TRUE = 1;
    private static final String FEEDBACK_STATUS_PUBLISHABLE = "publishable";
    private static final String FEEDBACK_STATUS_AWAITING_CAPTAIN = "awaiting_captain";
    private static final String FEEDBACK_STATUS_COMMENT_MISSING = "comment_missing";
    private static final String FEEDBACK_STATUS_COMMENT_SHORT = "comment_short";
    private static final String JUDGE_ANOMALY_NOT_SUBMITTED = "not_submitted";
    private static final String JUDGE_ANOMALY_COMMENT_MISSING = "comment_missing";
    private static final String JUDGE_ANOMALY_COMMENT_SHORT = "comment_short";
    private static final String GROUP_CURRENT = "current";
    private static final String GROUP_REQUIRED = "required";
    private static final String GROUP_FUTURE = "future";
    private static final String GROUP_LOCKED = "locked";
    private static final String GROUP_DATA_ISSUE = "data_issue";
    private static final int COMPETITION_CODE_RETRY_LIMIT = 5;
    private static final int CODE_SUFFIX_BOUND = 36 * 36 * 36 * 36 * 36;
    private static final int CROSS_MIN_DIMENSIONS = 2;
    private static final int CROSS_MAX_DIMENSIONS = 3;
    private static final List<String> PROFESSIONAL_DIMENSION_LABELS = List.of("香气", "外观", "味道", "口感", "整体印象");
    private static final List<BigDecimal> PROFESSIONAL_DIMENSION_MAX_SCORES = List.of(
            BigDecimal.valueOf(12),
            BigDecimal.valueOf(3),
            BigDecimal.valueOf(20),
            BigDecimal.valueOf(5),
            BigDecimal.valueOf(10));
    private static final int DEFAULT_MIN_COMMENT_LENGTH = 0;
    private static final String COMPETITION_CODE_PREFIX = "BC";
    private static final String DEFAULT_DELIVERY_METHOD = "BOTH";
    private static final String DEFAULT_LOGISTICS_VISIBILITY = "PAYMENT_CONFIRMED";
    private static final String LOG_TARGET_COMPETITION = "COMPETITION";
    private static final DateTimeFormatter COMPETITION_CODE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter EXPORT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> ENTRY_FIELD_TYPES = Set.of("text", "textarea", "number", "select", "multi_select");

    private final CompetitionMapper competitionMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;
    private final EntryFieldConfigMapper entryFieldConfigMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;
    private final BreweryMapper breweryMapper;
    private final AwardResultMapper awardResultMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundResultMapper roundResultMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final EntryDeliveryMapper entryDeliveryMapper;
    private final EntryRefundMapper entryRefundMapper;
    private final StyleLibraryService styleLibraryService;
    private final EntryScanLabelService entryScanLabelService;
    private final RoundService roundService;
    private final AwardService awardService;
    private final CompetitionAnalyticsService competitionAnalyticsService;
    private final ObjectMapper objectMapper;

    private record ExportSheet(String name, List<List<String>> rows) {
    }

    @Override
    public List<CompetitionVO> listCompetitions(boolean includeArchived) {
        // 1) 兜底关闭已到期报名，确保后台列表状态准确
        closeExpiredRegistrations(LocalDateTime.now());

        // 2) 查询比赛主数据，常用列表默认排除归档赛事
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<Competition>()
                .orderByDesc(Competition::getCompetitionDate)
                .orderByDesc(Competition::getId);
        if (!includeArchived) {
            wrapper.ne(Competition::getStatus, CompetitionStatus.ARCHIVED.name());
        }
        List<Competition> competitions = competitionMapper.selectList(wrapper);

        // 3) 聚合每场比赛的配置检查与统计摘要
        return competitions.stream()
                .map(competition -> {
                    CompetitionDetailVO detail = buildDetail(competition);
                    return toCompetitionVO(competition, detail);
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredRegistrations() {
        // 1) 使用单一时间点判定本次到期范围
        LocalDateTime now = LocalDateTime.now();

        // 2) 批量关闭已到报名截止时间的报名中赛事
        return closeExpiredRegistrations(now);
    }

    @Override
    public PortalHomeVO getPortalHome() {
        // 1) 查询厂商端可展示赛事
        List<PortalCompetitionVO> competitions = listPortalCompetitions();
        List<PortalCompetitionVO> openCompetitions = competitions.stream()
                .filter(item -> CompetitionStatus.REGISTRATION_OPEN.name().equals(item.getStatus()))
                .toList();

        // 2) 选择首页主赛事
        PortalCompetitionVO activeCompetition = openCompetitions.stream()
                .findFirst()
                .orElse(competitions.stream().findFirst().orElse(null));

        // 3) 组装首页数据
        return PortalHomeVO.builder()
                .activeCompetition(activeCompetition)
                .openCompetitions(openCompetitions)
                .competitions(competitions)
                .build();
    }

    @Override
    public List<PortalCompetitionVO> listPortalCompetitions() {
        // 1) 兜底关闭已到期报名，确保厂商端展示状态准确
        closeExpiredRegistrations(LocalDateTime.now());

        // 2) 查询非草稿、非归档赛事
        return competitionMapper.selectList(new LambdaQueryWrapper<Competition>()
                        .ne(Competition::getStatus, CompetitionStatus.DRAFT.name())
                        .ne(Competition::getStatus, CompetitionStatus.ARCHIVED.name())
                        .orderByDesc(Competition::getCompetitionDate)
                        .orderByDesc(Competition::getId))
                .stream()
                .map(this::toPortalCompetitionVO)
                .toList();
    }

    @Override
    public PortalCompetitionVO getPortalCompetitionDetail(Long id) {
        // 1) 兜底关闭已到期报名，确保厂商端详情状态准确
        closeExpiredRegistrations(LocalDateTime.now());

        // 2) 查询赛事并校验公开范围
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus status = parseStatus(competition);
        if (status == CompetitionStatus.DRAFT || status == CompetitionStatus.ARCHIVED) {
            throw new ResourceNotFoundException("赛事不存在");
        }

        // 3) 返回厂商端赛事配置
        return toPortalCompetitionVO(competition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionVO createCompetition(CompetitionCreateRequest request) {
        // 1) 参数规范化与完整性校验
        validateCreateRequest(request);
        List<EntryFieldItemRequest> entryFields = normalizeEntryFields(request.getEntryFields() == null ? List.of() : request.getEntryFields());
        String styleLibraryVersion = normalizeRequired(request.getStyleLibraryVersion(), "基础风格库不能为空");
        List<StyleItemVO> snapshotStyles = styleLibraryService.listEnabledStyles(styleLibraryVersion);
        List<ConfigNameItemRequest> categories = normalizeNameItems(resolveCreateCategories(request.getCategories(), snapshotStyles), "投递组别");
        validateScoreConfigs(request.getScoreConfigs());

        // 2) 构造草稿比赛主记录
        Competition competition = Competition.builder()
                .name(normalizeRequired(request.getName(), "比赛名称不能为空"))
                .competitionDate(request.getCompetitionDate())
                .registrationStart(request.getRegistrationStart())
                .registrationDeadline(request.getRegistrationDeadline())
                .status(CompetitionStatus.DRAFT.name())
                .competitionType(CompetitionType.of(request.getCompetitionType()).name())
                .entryFee(request.getEntryFee())
                .earlyBirdFee(request.getEarlyBirdFee())
                .earlyBirdDeadline(request.getEarlyBirdDeadline())
                .description(normalizeRequired(request.getDescription(), "赛事简介不能为空"))
                .rulesUrl(normalizeRulesUrl(request.getRulesUrl()))
                .styleLibraryVersion(styleLibraryVersion)
                .build();
        applyCreateLogistics(competition, request);

        // 3) 事务内写入比赛和新建页完整配置
        insertCompetitionWithGeneratedCode(competition);
        replaceCategories(competition.getId(), categories);
        replaceStyleSnapshot(competition.getId(), snapshotStyles);
        replaceEntryFields(competition.getId(), entryFields);
        replaceScoreConfigs(competition.getId(), request.getScoreConfigs());

        // 4) 返回列表口径摘要
        Competition saved = competitionMapper.selectById(competition.getId());
        return toCompetitionVO(saved, buildDetail(saved));
    }

    @Override
    public CompetitionDetailVO getCompetitionDetail(Long id) {
        // 1) 兜底关闭已到期报名，确保详情页状态准确
        closeExpiredRegistrations(LocalDateTime.now());

        // 2) 查询比赛主数据
        Competition competition = getCompetitionOrThrow(id);

        // 3) 查询并聚合关联配置
        return buildDetail(competition);
    }

    @Override
    public CompetitionAnalyticsVO getCompetitionAnalytics(Long competitionId) {
        getCompetitionOrThrow(competitionId);
        return competitionAnalyticsService.buildAnalytics(competitionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCompetition(Long id) {
        // 1) 查询比赛并确认当前阶段
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus status = parseStatus(competition);
        if (status == CompetitionStatus.ARCHIVED) {
            throw new BaseException("比赛已归档");
        }

        // 2) 草稿且无业务数据时可移除；进入流程后保留台账并归档
        boolean hasBusinessData = hasEntries(id)
                || competitionRoundMapper.selectCount(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, id)) > 0;
        if (status == CompetitionStatus.DRAFT && !hasBusinessData) {
            competitionMapper.deleteById(id);
            return;
        }
        competition.setStatus(CompetitionStatus.ARCHIVED.name());
        competitionMapper.updateById(competition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateBaseInfo(Long id, CompetitionBaseInfoUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        validateEarlyBirdConfig(request.getEarlyBirdFee(), request.getEarlyBirdDeadline(),
                request.getEntryFee(), request.getRegistrationStart(), request.getRegistrationDeadline());
        assertBaseInfoEditable(competition, request);
        String nextCode = normalizeRequired(request.getCode(), "比赛编号不能为空");
        assertCompetitionCodeUnique(nextCode, id);

        // 2) 应用允许修改的基础字段
        competition.setName(normalizeRequired(request.getName(), "比赛名称不能为空"));
        competition.setCode(nextCode);
        competition.setCompetitionDate(request.getCompetitionDate());
        competition.setRegistrationStart(request.getRegistrationStart());
        competition.setRegistrationDeadline(request.getRegistrationDeadline());
        competition.setEntryFee(request.getEntryFee());
        competition.setEarlyBirdFee(request.getEarlyBirdFee());
        competition.setEarlyBirdDeadline(request.getEarlyBirdDeadline());
        competition.setDescription(normalizeRequired(request.getDescription(), "赛事简介不能为空"));
        competition.setRulesUrl(normalizeRulesUrl(request.getRulesUrl()));
        applyBaseInfoLogistics(competition, request);

        // 3) 更新比赛主记录
        try {
            competitionMapper.updateById(competition);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("比赛编号已存在");
        }

        // 4) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateCategories(Long id, ConfigNameBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertEntryStructureEditable(competition);
        List<ConfigNameItemRequest> items = normalizeNameItems(request.getItems(), "投递组别");

        // 2) 替换当前比赛投递组别
        replaceCategories(id, items);

        // 3) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateStyles(Long id, CompetitionStyleLibraryUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertEntryStructureEditable(competition);
        String styleLibraryVersion = normalizeRequired(request.getStyleLibraryVersion(), "基础风格库不能为空");
        List<StyleItemVO> snapshotStyles = styleLibraryService.listEnabledStyles(styleLibraryVersion);
        competition.setStyleLibraryVersion(styleLibraryVersion);

        // 2) 更新比赛风格库版本并重建比赛风格快照
        competitionMapper.updateById(competition);
        replaceStyleSnapshot(id, snapshotStyles);

        // 3) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    private void replaceStyleSnapshot(Long competitionId, List<StyleItemVO> styles) {
        // 1) 清理当前比赛旧基础风格快照
        competitionStyleConfigMapper.delete(new LambdaQueryWrapper<CompetitionStyleConfig>()
                .eq(CompetitionStyleConfig::getCompetitionId, competitionId));

        // 2) 批量写入新的比赛风格快照
        int sort = 0;
        for (StyleItemVO style : styles) {
            competitionStyleConfigMapper.insert(CompetitionStyleConfig.builder()
                    .competitionId(competitionId)
                    .name(style.getName())
                    .categoryName(style.getCategoryName())
                    .styleCode(style.getStyleCode())
                    .description(style.getDescription())
                    .sortOrder(sort++)
                    .build());
        }
    }

    private List<ConfigNameItemRequest> resolveCreateCategories(List<ConfigNameItemRequest> categories,
                                                                List<StyleItemVO> snapshotStyles) {
        if (categories != null && categories.stream().anyMatch(item -> StringUtils.hasText(item.getName()))) {
            return categories;
        }
        List<ConfigNameItemRequest> generated = new ArrayList<>();
        for (int index = 0; index < snapshotStyles.size(); index++) {
            StyleItemVO style = snapshotStyles.get(index);
            ConfigNameItemRequest item = new ConfigNameItemRequest();
            item.setName(style.getName());
            item.setSortOrder(resolveSort(style.getSortOrder(), index));
            generated.add(item);
        }
        return generated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateEntryFields(Long id, EntryFieldBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertEntryStructureEditable(competition);
        List<EntryFieldItemRequest> items = normalizeEntryFields(request.getItems());

        // 2) 替换当前比赛报名补充字段
        replaceEntryFields(id, items);

        // 3) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateJudgeTables(Long id, JudgeTableBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertJudgeConfigEditable(competition);
        if (competitionRoundMapper.selectCount(new LambdaQueryWrapper<com.beercompetition.pojo.po.CompetitionRound>()
                .eq(com.beercompetition.pojo.po.CompetitionRound::getCompetitionId, id)) > 0) {
            throw new BaseException("已创建轮次后不能修改基础评审桌");
        }
        List<JudgeTableItemRequest> items = normalizeJudgeTables(request.getItems());

        // 2) 清理当前比赛旧评审桌
        judgeAssignmentMapper.delete(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, id));
        judgeTableMapper.delete(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, id));

        // 3) 批量写入新评审桌
        int sort = 0;
        for (JudgeTableItemRequest item : items) {
            judgeTableMapper.insert(JudgeTable.builder()
                    .competitionId(id)
                    .tableName(item.getTableName())
                    .sortOrder(sort++)
                    .build());
        }

        // 4) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO openRegistration(Long id) {
        // 1) 查询比赛并校验状态流转入口
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus status = parseStatus(competition);
        if (status != CompetitionStatus.DRAFT) {
            throw new BaseException("只有草稿状态的比赛可以开放报名");
        }

        // 2) 执行完整性检查
        CompetitionDetailVO detail = buildDetail(competition);
        List<CompetitionCheckVO> blockingChecks = detail.getChecks().stream()
                .filter(check -> isBlockingCheck(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .toList();
        if (!blockingChecks.isEmpty()) {
            String message = blockingChecks.stream()
                    .map(CompetitionCheckVO::getLabel)
                    .collect(Collectors.joining("、"));
            throw new BaseException("配置未完整，暂不能开放报名：" + message);
        }

        // 3) 更新比赛状态为报名中
        CompetitionStatus oldStatus = status;
        competition.setStatus(CompetitionStatus.REGISTRATION_OPEN.name());
        competitionMapper.updateById(competition);
        writeCompetitionStageLog("COMPETITION_OPEN_REGISTRATION", competition.getId(), oldStatus,
                CompetitionStatus.REGISTRATION_OPEN, "开放报名", null, null, competition.getRegistrationDeadline());

        // 4) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO closeRegistration(Long id) {
        // 1) 查询比赛并校验状态流转入口
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus oldStatus = parseStatus(competition);
        if (oldStatus != CompetitionStatus.REGISTRATION_OPEN) {
            throw new BaseException("只有报名中的比赛可以截止报名");
        }

        // 2) 更新比赛状态为报名截止
        competition.setStatus(CompetitionStatus.REGISTRATION_CLOSED.name());
        competitionMapper.updateById(competition);
        writeCompetitionStageLog("COMPETITION_CLOSE_REGISTRATION", competition.getId(), oldStatus,
                CompetitionStatus.REGISTRATION_CLOSED, "截止报名", null, competition.getRegistrationDeadline(),
                competition.getRegistrationDeadline());

        // 3) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO prepareJudging(Long id) {
        // 1) 查询比赛并校验状态流转入口
        Competition competition = getCompetitionOrThrow(id);
        if (parseStatus(competition) != CompetitionStatus.REGISTRATION_CLOSED) {
            throw new BaseException("只有报名截止后的比赛可以进入评审准备");
        }

        // 2) 校验评审准备必需配置
        CompetitionDetailVO detail = buildDetail(competition);
        assertChecksDone(detail.getChecks(), Set.of("judgeTables", "scoreForms", "storedEntries"), "评审准备");

        // 3) 更新比赛状态为评审准备
        CompetitionStatus oldStatus = parseStatus(competition);
        competition.setStatus(CompetitionStatus.JUDGING_PREP.name());
        competitionMapper.updateById(competition);
        writeCompetitionStageLog("COMPETITION_PREPARE_JUDGING", competition.getId(), oldStatus,
                CompetitionStatus.JUDGING_PREP, "进入评审准备", null, competition.getRegistrationDeadline(),
                competition.getRegistrationDeadline());

        // 4) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO reopenRegistration(Long id, CompetitionReopenRegistrationRequest request) {
        // 1) 查询比赛并校验恢复入口
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus oldStatus = parseStatus(competition);
        if (oldStatus != CompetitionStatus.REGISTRATION_CLOSED) {
            throw new BaseException("只有已截止报名的比赛可以重新开放报名");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldDeadline = competition.getRegistrationDeadline();
        LocalDateTime nextDeadline = request.getRegistrationDeadline();
        if ((oldDeadline == null || !oldDeadline.isAfter(now)) && nextDeadline == null) {
            throw new BaseException("原报名截止时间已过，请设置新的报名截止时间");
        }
        if (nextDeadline != null) {
            validateReopenRegistrationDeadline(competition, nextDeadline, now);
        }

        // 2) 更新报名窗口和比赛状态
        if (nextDeadline != null) {
            competition.setRegistrationDeadline(nextDeadline);
        }
        competition.setStatus(CompetitionStatus.REGISTRATION_OPEN.name());
        competitionMapper.updateById(competition);

        // 3) 写入阶段恢复审计并返回详情
        writeCompetitionStageLog("COMPETITION_REOPEN_REGISTRATION", competition.getId(), oldStatus,
                CompetitionStatus.REGISTRATION_OPEN, "重新开放报名", request.getReason(), oldDeadline,
                competition.getRegistrationDeadline());
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO returnToSampleCheck(Long id, CompetitionReturnToSampleCheckRequest request) {
        // 1) 查询比赛并校验恢复入口
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus oldStatus = parseStatus(competition);
        if (oldStatus != CompetitionStatus.JUDGING_PREP) {
            throw new BaseException("只有评审准备中可以退回样品入库核对");
        }
        CompetitionRound firstRound = competitionRoundMapper.selectOne(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, id)
                .eq(CompetitionRound::getRoundNo, 1)
                .last("LIMIT 1"));
        if (firstRound != null && !RoundStatus.DRAFT.name().equals(firstRound.getStatus())) {
            throw new BaseException("首轮已发布，不能退回样品入库核对");
        }

        // 2) 只回退比赛主状态，保留第一轮草稿和分桌草稿
        competition.setStatus(CompetitionStatus.REGISTRATION_CLOSED.name());
        competitionMapper.updateById(competition);

        // 3) 写入阶段恢复审计并返回详情
        writeCompetitionStageLog("COMPETITION_RETURN_TO_SAMPLE_CHECK", competition.getId(), oldStatus,
                CompetitionStatus.REGISTRATION_CLOSED, "退回样品入库核对", request.getReason(),
                competition.getRegistrationDeadline(), competition.getRegistrationDeadline());
        return getCompetitionDetail(id);
    }

    @Override
    public List<ScoreConfigVO> getScoreConfigs(Long competitionId) {
        // 1) 校验比赛存在
        getCompetitionOrThrow(competitionId);

        // 2) 查询评分表配置
        return listScoreConfigs(competitionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ScoreConfigVO> updateScoreConfigs(Long competitionId, ScoreConfigBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(competitionId);
        assertJudgeConfigEditable(competition);
        validateScoreConfigs(request.getConfigs());

        // 2) 替换当前比赛评分表
        replaceScoreConfigs(competitionId, request.getConfigs());

        // 3) 返回最新评分表配置
        return listScoreConfigs(competitionId);
    }

    @Override
    public List<AdminFeedbackReviewEntryVO> getFeedbackReviewEntries(Long competitionId) {
        // 1) 校验比赛并定位第一轮评分制轮次
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
            return List.of();
        }

        // 2) 查询第一轮桌次、酒款、评审、标签和评分记录
        List<RoundTable> tables = roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getRoundId, firstScoreRound.getId())
                .orderByAsc(RoundTable::getSortOrder)
                .orderByAsc(RoundTable::getId));
        if (tables.isEmpty()) {
            return List.of();
        }
        List<Long> tableIds = tables.stream().map(RoundTable::getId).toList();
        List<RoundTableEntry> roundEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, firstScoreRound.getId())
                .orderByAsc(RoundTableEntry::getRoundTableId)
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
        if (roundEntries.isEmpty()) {
            return List.of();
        }
        Set<Long> beerEntryIds = roundEntries.stream().map(RoundTableEntry::getBeerEntryId).collect(Collectors.toCollection(LinkedHashSet::new));
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
        return roundEntries.stream()
                .map(roundEntry -> buildFeedbackReviewEntry(firstScoreRound, roundEntry, tableById, entryById,
                        labelByEntryId, categoryNameById, membersByTable, judgeById, personalScoreByJudgeEntry,
                        finalScoreByEntry, minCommentLengthByRole, feedbackEditable))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public byte[] exportScoringData(Long competitionId) {
        // 1) 查询比赛、酒款和导出关联数据
        getCompetitionOrThrow(competitionId);
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .ne(BeerEntry::getStatus, EntryStatus.CANCELED.name())
                .orderByAsc(BeerEntry::getCategoryId)
                .orderByAsc(BeerEntry::getId));
        Map<Long, CompetitionCategory> categoryById = competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId))
                .stream()
                .collect(Collectors.toMap(CompetitionCategory::getId, Function.identity(), (left, right) -> left));
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entries.stream().map(BeerEntry::getId).toList());
        Map<Long, List<ScoreRecord>> scoresByEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, competitionId)
                        .orderByAsc(ScoreRecord::getBeerEntryId)
                        .orderByAsc(ScoreRecord::getFinalFlag)
                        .orderByAsc(ScoreRecord::getId))
                .stream()
                .collect(Collectors.groupingBy(ScoreRecord::getBeerEntryId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, JudgeAccount> judgeById = loadJudgeAccounts(scoresByEntry.values().stream()
                .flatMap(List::stream)
                .map(ScoreRecord::getJudgeAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        List<RoundResult> roundResults = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getCompetitionId, competitionId)
                .orderByAsc(RoundResult::getRoundId)
                .orderByAsc(RoundResult::getRoundTableId)
                .orderByAsc(RoundResult::getRankNo)
                .orderByAsc(RoundResult::getId));
        Map<Long, List<RoundResult>> roundResultsByEntry = roundResults.stream()
                .collect(Collectors.groupingBy(RoundResult::getBeerEntryId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<AwardResult>> awardsByEntry = awardResultMapper.selectList(new LambdaQueryWrapper<AwardResult>()
                        .eq(AwardResult::getCompetitionId, competitionId)
                        .orderByDesc(AwardResult::getChampionFlag)
                        .orderByAsc(AwardResult::getRankNo)
                        .orderByAsc(AwardResult::getId))
                .stream()
                .collect(Collectors.groupingBy(AwardResult::getBeerEntryId, LinkedHashMap::new, Collectors.toList()));
        Set<Long> exportRoundIds = roundResults.stream()
                .map(RoundResult::getRoundId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        awardsByEntry.values().stream()
                .flatMap(List::stream)
                .map(AwardResult::getSourceRoundId)
                .filter(Objects::nonNull)
                .forEach(exportRoundIds::add);
        Set<Long> exportTableIds = roundResults.stream()
                .map(RoundResult::getRoundTableId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        awardsByEntry.values().stream()
                .flatMap(List::stream)
                .map(AwardResult::getSourceRoundTableId)
                .filter(Objects::nonNull)
                .forEach(exportTableIds::add);
        Map<Long, CompetitionRound> roundById = loadRounds(exportRoundIds);
        Map<Long, RoundTable> tableById = loadRoundTables(exportTableIds);
        Map<Long, Brewery> breweryById = loadBreweries(entries.stream()
                .map(BeerEntry::getBreweryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, List<BeerEntryExtraField>> extraFieldsByEntry = loadExtraFields(entries.stream()
                .map(BeerEntry::getId)
                .collect(Collectors.toSet()));
        Map<Long, BeerEntry> entryById = entries.stream()
                .collect(Collectors.toMap(BeerEntry::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        // 2) 按工作人员实际复盘场景拆分工作表
        List<ExportSheet> sheets = List.of(
                new ExportSheet("酒款总览", buildEntryOverviewRows(entries, categoryById, labelByEntryId, breweryById,
                        extraFieldsByEntry, scoresByEntry, roundResultsByEntry, roundById, tableById, awardsByEntry)),
                new ExportSheet("评审原始评分", buildPersonalScoreRows(entries, categoryById, labelByEntryId, scoresByEntry, judgeById)),
                new ExportSheet("评分维度明细", buildScoreDimensionRows(entries, labelByEntryId, scoresByEntry, judgeById)),
                new ExportSheet("桌长汇总", buildCaptainSummaryRows(entries, categoryById, labelByEntryId, scoresByEntry)),
                new ExportSheet("轮次与奖项", buildRoundAwardRows(roundResults, awardsByEntry, entryById, categoryById, labelByEntryId, roundById, tableById))
        );

        // 3) 返回可被 Excel 直接打开的 xlsx 文件
        return buildXlsx(sheets);
    }

    private List<List<String>> buildEntryOverviewRows(List<BeerEntry> entries,
                                                       Map<Long, CompetitionCategory> categoryById,
                                                       Map<Long, EntryScanLabel> labelByEntryId,
                                                       Map<Long, Brewery> breweryById,
                                                       Map<Long, List<BeerEntryExtraField>> extraFieldsByEntry,
                                                       Map<Long, List<ScoreRecord>> scoresByEntry,
                                                       Map<Long, List<RoundResult>> roundResultsByEntry,
                                                       Map<Long, CompetitionRound> roundById,
                                                       Map<Long, RoundTable> tableById,
                                                       Map<Long, List<AwardResult>> awardsByEntry) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("匿名编号", "短编号", "酒款名称", "厂牌", "联系人", "投递组别", "基础风格", "酒精度",
                "额外报名信息", "报名状态", "入库状态", "发布状态", "原始评分数", "桌长共识分",
                "桌长综合评语", "轮次记录", "奖项结果"));
        for (BeerEntry entry : entries) {
            Brewery brewery = breweryById.get(entry.getBreweryId());
            List<ScoreRecord> scoreRecords = scoresByEntry.getOrDefault(entry.getId(), List.of());
            ScoreRecord finalScore = findFinalScore(scoreRecords);
            rows.add(List.of(
                    anonymousCode(entry, labelByEntryId),
                    shortCode(entry, labelByEntryId),
                    firstText(entry.getName(), ""),
                    brewery == null ? "" : firstText(brewery.getCompanyName(), ""),
                    brewery == null ? "" : firstText(brewery.getContactName(), ""),
                    categoryName(entry, categoryById),
                    firstText(entry.getStyle(), ""),
                    toPlain(entry.getAbv()),
                    formatExtraFields(extraFieldsByEntry.getOrDefault(entry.getId(), List.of())),
                    formatEntryStatus(entry.getStatus()),
                    Objects.equals(entry.getStoredFlag(), FLAG_TRUE) ? "已入库" : "未入库",
                    EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus()) ? "已发布" : "未发布",
                    String.valueOf(scoreRecords.stream().filter(record -> !Objects.equals(record.getFinalFlag(), FLAG_TRUE)).count()),
                    finalScore == null ? "" : toPlain(firstScore(finalScore.getConsensusScore(), finalScore.getTotalScore())),
                    finalScore == null ? "" : firstText(finalScore.getComments(), ""),
                    formatRoundResults(roundResultsByEntry.getOrDefault(entry.getId(), List.of()), roundById, tableById),
                    formatAwards(awardsByEntry.getOrDefault(entry.getId(), List.of()))
            ));
        }
        return rows;
    }

    private List<List<String>> buildPersonalScoreRows(List<BeerEntry> entries,
                                                      Map<Long, CompetitionCategory> categoryById,
                                                      Map<Long, EntryScanLabel> labelByEntryId,
                                                      Map<Long, List<ScoreRecord>> scoresByEntry,
                                                      Map<Long, JudgeAccount> judgeById) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("匿名编号", "短编号", "酒款名称", "投递组别", "基础风格", "评审", "评审角色",
                "总分", "各维度评分", "文字反馈", "提交时间"));
        for (BeerEntry entry : entries) {
            for (ScoreRecord record : scoresByEntry.getOrDefault(entry.getId(), List.of())) {
                if (Objects.equals(record.getFinalFlag(), FLAG_TRUE)) {
                    continue;
                }
                JudgeAccount judge = judgeById.get(record.getJudgeAccountId());
                rows.add(List.of(
                        anonymousCode(entry, labelByEntryId),
                        shortCode(entry, labelByEntryId),
                        firstText(entry.getName(), ""),
                        categoryName(entry, categoryById),
                        firstText(entry.getStyle(), ""),
                        judge == null ? "未知评审" : firstText(judge.getName(), "未知评审"),
                        feedbackRoleLabel(record.getJudgeRoleType()),
                        toPlain(record.getTotalScore()),
                        formatScoreDimensions(record.getDimensionsJson()),
                        firstText(record.getComments(), ""),
                        formatDateTime(firstTime(record.getUpdateTime(), record.getCreateTime()))
                ));
            }
        }
        return rows;
    }

    private List<List<String>> buildScoreDimensionRows(List<BeerEntry> entries,
                                                       Map<Long, EntryScanLabel> labelByEntryId,
                                                       Map<Long, List<ScoreRecord>> scoresByEntry,
                                                       Map<Long, JudgeAccount> judgeById) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("匿名编号", "短编号", "酒款名称", "评审", "评审角色", "评分维度", "得分", "满分", "维度备注"));
        for (BeerEntry entry : entries) {
            for (ScoreRecord record : scoresByEntry.getOrDefault(entry.getId(), List.of())) {
                if (Objects.equals(record.getFinalFlag(), FLAG_TRUE)) {
                    continue;
                }
                JudgeAccount judge = judgeById.get(record.getJudgeAccountId());
                String judgeName = judge == null ? "未知评审" : firstText(judge.getName(), "未知评审");
                for (DimensionRequest dimension : readDimensions(record.getDimensionsJson())) {
                    rows.add(List.of(
                            anonymousCode(entry, labelByEntryId),
                            shortCode(entry, labelByEntryId),
                            firstText(entry.getName(), ""),
                            judgeName,
                            feedbackRoleLabel(record.getJudgeRoleType()),
                            firstText(dimension.getLabel(), ""),
                            toPlain(dimension.getScore()),
                            toPlain(dimension.getMaxScore()),
                            firstText(dimension.getNote(), "")
                    ));
                }
            }
        }
        return rows;
    }

    private List<List<String>> buildCaptainSummaryRows(List<BeerEntry> entries,
                                                       Map<Long, CompetitionCategory> categoryById,
                                                       Map<Long, EntryScanLabel> labelByEntryId,
                                                       Map<Long, List<ScoreRecord>> scoresByEntry) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("匿名编号", "短编号", "酒款名称", "投递组别", "基础风格", "桌长共识分", "是否晋级",
                "桌长综合评语", "提交时间"));
        for (BeerEntry entry : entries) {
            ScoreRecord finalScore = findFinalScore(scoresByEntry.getOrDefault(entry.getId(), List.of()));
            rows.add(List.of(
                    anonymousCode(entry, labelByEntryId),
                    shortCode(entry, labelByEntryId),
                    firstText(entry.getName(), ""),
                    categoryName(entry, categoryById),
                    firstText(entry.getStyle(), ""),
                    finalScore == null ? "" : toPlain(firstScore(finalScore.getConsensusScore(), finalScore.getTotalScore())),
                    finalScore != null && Objects.equals(finalScore.getAdvancedFlag(), FLAG_TRUE) ? "是" : "否",
                    finalScore == null ? "" : firstText(finalScore.getComments(), ""),
                    finalScore == null ? "" : formatDateTime(firstTime(finalScore.getUpdateTime(), finalScore.getCreateTime()))
            ));
        }
        return rows;
    }

    private List<List<String>> buildRoundAwardRows(List<RoundResult> roundResults,
                                                   Map<Long, List<AwardResult>> awardsByEntry,
                                                   Map<Long, BeerEntry> entryById,
                                                   Map<Long, CompetitionCategory> categoryById,
                                                   Map<Long, EntryScanLabel> labelByEntryId,
                                                   Map<Long, CompetitionRound> roundById,
                                                   Map<Long, RoundTable> tableById) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("记录类型", "匿名编号", "短编号", "酒款名称", "投递组别", "轮次", "评审桌",
                "结果口径", "排名", "锁定状态", "奖项名称", "奖项状态", "发布时间"));
        for (RoundResult result : roundResults) {
            BeerEntry entry = entryById.get(result.getBeerEntryId());
            if (entry == null) {
                continue;
            }
            CompetitionRound round = roundById.get(result.getRoundId());
            RoundTable table = tableById.get(result.getRoundTableId());
            rows.add(List.of(
                    "轮次记录",
                    anonymousCode(entry, labelByEntryId),
                    shortCode(entry, labelByEntryId),
                    firstText(entry.getName(), ""),
                    categoryName(entry, categoryById),
                    round == null ? "" : firstText(round.getRoundName(), ""),
                    table == null ? "" : firstText(table.getTableName(), ""),
                    formatRoundResultType(result.getResultType(), result.getSlotLabel()),
                    result.getRankNo() == null ? "" : String.valueOf(result.getRankNo()),
                    Objects.equals(result.getLockedFlag(), FLAG_TRUE) ? "已锁定" : "待确认",
                    "",
                    "",
                    ""
            ));
        }
        for (List<AwardResult> awards : awardsByEntry.values()) {
            for (AwardResult award : awards) {
                BeerEntry entry = entryById.get(award.getBeerEntryId());
                if (entry == null) {
                    continue;
                }
                CompetitionRound round = roundById.get(award.getSourceRoundId());
                RoundTable table = tableById.get(award.getSourceRoundTableId());
                rows.add(List.of(
                        "奖项结果",
                        anonymousCode(entry, labelByEntryId),
                        shortCode(entry, labelByEntryId),
                        firstText(entry.getName(), ""),
                        categoryName(entry, categoryById),
                        round == null ? "" : firstText(round.getRoundName(), ""),
                        table == null ? "" : firstText(table.getTableName(), ""),
                        Objects.equals(award.getChampionFlag(), FLAG_TRUE) ? "全场总冠军" : "组别奖项",
                        award.getRankNo() == null ? "" : String.valueOf(award.getRankNo()),
                        "",
                        firstText(award.getAwardName(), ""),
                        formatAwardResultStatus(award.getStatus()),
                        formatDateTime(award.getPublishedTime())
                ));
            }
        }
        return rows;
    }

    private Map<Long, Brewery> loadBreweries(Set<Long> breweryIds) {
        if (breweryIds.isEmpty()) {
            return Map.of();
        }
        return breweryMapper.selectBatchIds(breweryIds).stream()
                .collect(Collectors.toMap(Brewery::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, List<BeerEntryExtraField>> loadExtraFields(Set<Long> entryIds) {
        if (entryIds.isEmpty()) {
            return Map.of();
        }
        return beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .in(BeerEntryExtraField::getBeerEntryId, entryIds)
                        .orderByAsc(BeerEntryExtraField::getBeerEntryId)
                        .orderByAsc(BeerEntryExtraField::getId))
                .stream()
                .collect(Collectors.groupingBy(BeerEntryExtraField::getBeerEntryId, LinkedHashMap::new, Collectors.toList()));
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
        return !CompetitionStatus.PUBLISHED.name().equals(competition.getStatus())
                && !CompetitionStatus.ARCHIVED.name().equals(competition.getStatus());
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

    private Map<Long, CompetitionRound> loadRounds(Set<Long> roundIds) {
        if (roundIds.isEmpty()) {
            return Map.of();
        }
        return competitionRoundMapper.selectBatchIds(roundIds).stream()
                .collect(Collectors.toMap(CompetitionRound::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, RoundTable> loadRoundTables(Set<Long> tableIds) {
        if (tableIds.isEmpty()) {
            return Map.of();
        }
        return roundTableMapper.selectBatchIds(tableIds).stream()
                .collect(Collectors.toMap(RoundTable::getId, Function.identity(), (left, right) -> left));
    }

    private ScoreRecord findFinalScore(List<ScoreRecord> scoreRecords) {
        return scoreRecords.stream()
                .filter(record -> Objects.equals(record.getFinalFlag(), FLAG_TRUE))
                .findFirst()
                .orElse(null);
    }

    private String anonymousCode(BeerEntry entry, Map<Long, EntryScanLabel> labelByEntryId) {
        EntryScanLabel label = labelByEntryId.get(entry.getId());
        return firstText(label == null ? null : label.getLabelCode(), entry.getUuid());
    }

    private String shortCode(BeerEntry entry, Map<Long, EntryScanLabel> labelByEntryId) {
        EntryScanLabel label = labelByEntryId.get(entry.getId());
        return label == null ? "" : firstText(label.getShortCode(), "");
    }

    private String categoryName(BeerEntry entry, Map<Long, CompetitionCategory> categoryById) {
        CompetitionCategory category = categoryById.get(entry.getCategoryId());
        return category == null ? "" : firstText(category.getName(), "");
    }

    private String formatExtraFields(List<BeerEntryExtraField> extraFields) {
        return extraFields.stream()
                .filter(field -> StringUtils.hasText(field.getFieldValue()))
                .map(field -> firstText(field.getFieldLabel(), field.getFieldKey()) + "：" + normalizeInline(field.getFieldValue()))
                .collect(Collectors.joining("；"));
    }

    private String formatEntryStatus(String status) {
        if (EntryStatus.PENDING_PAYMENT.name().equals(status)) {
            return "待支付";
        }
        if (EntryStatus.REGISTERED.name().equals(status)) {
            return "已支付，报名成功";
        }
        if (EntryStatus.STORED.name().equals(status)) {
            return "已入库";
        }
        if (EntryStatus.CANCELED.name().equals(status)) {
            return "已取消";
        }
        if (EntryStatus.RESULT_PUBLISHED.name().equals(status)) {
            return "结果已出";
        }
        return firstText(status, "");
    }

    private String formatRoundResultType(String resultType, String slotLabel) {
        String label = switch (firstText(resultType, "")) {
            case "ADVANCE" -> "晋级";
            case "RANK" -> "排序";
            case "AWARD_CANDIDATE" -> "奖项候选";
            case "CHAMPION" -> "总冠军候选";
            default -> firstText(resultType, "");
        };
        return StringUtils.hasText(slotLabel) ? slotLabel + "（" + label + "）" : label;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(EXPORT_TIME_FORMAT);
    }

    private String formatScoreDimensions(String dimensionsJson) {
        return readDimensions(dimensionsJson).stream()
                .map(item -> item.getLabel() + " " + toPlain(item.getScore()) + "/" + toPlain(item.getMaxScore()))
                .collect(Collectors.joining("，"));
    }

    private String formatRoundResults(List<RoundResult> results,
                                      Map<Long, CompetitionRound> roundById,
                                      Map<Long, RoundTable> tableById) {
        return results.stream()
                .map(result -> {
                    CompetitionRound round = roundById.get(result.getRoundId());
                    RoundTable table = tableById.get(result.getRoundTableId());
                    return firstText(round == null ? null : round.getRoundName(), "轮次")
                            + " " + firstText(table == null ? null : table.getTableName(), "评审桌")
                            + " " + firstText(result.getSlotLabel(), result.getResultType())
                            + (result.getRankNo() == null ? "" : " 第" + result.getRankNo() + "名")
                            + (Objects.equals(result.getLockedFlag(), 1) ? " 已锁定" : " 待确认");
                })
                .collect(Collectors.joining("; "));
    }

    private String formatAwards(List<AwardResult> awards) {
        return awards.stream()
                .map(award -> award.getAwardName() + " " + formatAwardResultStatus(award.getStatus()))
                .collect(Collectors.joining("; "));
    }

    private String formatAwardResultStatus(String status) {
        if (AwardResultStatus.PUBLISHED.name().equals(status)) {
            return "已发布";
        }
        if (AwardResultStatus.CONFIRMED.name().equals(status)) {
            return "已确认";
        }
        return "待确认";
    }

    private byte[] buildXlsx(List<ExportSheet> sheets) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                writeZipEntry(zip, "[Content_Types].xml", buildContentTypesXml(sheets.size()));
                writeZipEntry(zip, "_rels/.rels", """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                        </Relationships>
                        """);
                writeZipEntry(zip, "xl/workbook.xml", buildWorkbookXml(sheets));
                writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelationshipsXml(sheets.size()));
                writeZipEntry(zip, "xl/styles.xml", """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                          <fonts count="2"><font/><font><b/></font></fonts>
                          <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
                          <borders count="1"><border/></borders>
                          <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                          <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/></cellXfs>
                        </styleSheet>
                        """);
                for (int index = 0; index < sheets.size(); index++) {
                    writeZipEntry(zip, "xl/worksheets/sheet" + (index + 1) + ".xml", buildSheetXml(sheets.get(index).rows()));
                }
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BaseException("导出评分数据失败");
        }
    }

    private String buildContentTypesXml(int sheetCount) {
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                """);
        for (int index = 1; index <= sheetCount; index++) {
            xml.append("  <Override PartName=\"/xl/worksheets/sheet")
                    .append(index)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n");
        }
        return xml.append("</Types>").toString();
    }

    private String buildWorkbookXml(List<ExportSheet> sheets) {
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                """);
        for (int index = 0; index < sheets.size(); index++) {
            xml.append("    <sheet name=\"")
                    .append(escapeXml(sheets.get(index).name()))
                    .append("\" sheetId=\"")
                    .append(index + 1)
                    .append("\" r:id=\"rId")
                    .append(index + 1)
                    .append("\"/>\n");
        }
        return xml.append("  </sheets></workbook>").toString();
    }

    private String buildWorkbookRelationshipsXml(int sheetCount) {
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                """);
        for (int index = 1; index <= sheetCount; index++) {
            xml.append("  <Relationship Id=\"rId")
                    .append(index)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(index)
                    .append(".xml\"/>\n");
        }
        xml.append("  <Relationship Id=\"rId")
                .append(sheetCount + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n");
        return xml.append("</Relationships>").toString();
    }

    private void writeZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String buildSheetXml(List<List<String>> rows) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
                .append("<cols>")
                .append("<col min=\"1\" max=\"5\" width=\"18\" customWidth=\"1\"/>")
                .append("<col min=\"6\" max=\"10\" width=\"36\" customWidth=\"1\"/>")
                .append("<col min=\"11\" max=\"11\" width=\"12\" customWidth=\"1\"/>")
                .append("</cols><sheetData>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            int rowNo = rowIndex + 1;
            xml.append("<row r=\"").append(rowNo).append("\">");
            List<String> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                String ref = columnName(columnIndex + 1) + rowNo;
                xml.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"");
                if (rowIndex == 0) {
                    xml.append(" s=\"1\"");
                }
                xml.append("><is><t>")
                        .append(escapeXml(row.get(columnIndex)))
                        .append("</t></is></c>");
            }
            xml.append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private String columnName(int columnNo) {
        StringBuilder name = new StringBuilder();
        int value = columnNo;
        while (value > 0) {
            value--;
            name.insert(0, (char) ('A' + (value % 26)));
            value /= 26;
        }
        return name.toString();
    }

    private String escapeXml(String value) {
        String text = value == null ? "" : value;
        StringBuilder escaped = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            switch (ch) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> {
                    if (ch == '\t' || ch == '\n' || ch == '\r' || ch >= 0x20) {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : (fallback == null ? "" : fallback);
    }

    private String normalizeInline(String text) {
        return text == null ? "" : text.replace("\r", " ").replace("\n", " ").trim();
    }

    private String toPlain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private void replaceCategories(Long competitionId, List<ConfigNameItemRequest> items) {
        competitionCategoryMapper.delete(new LambdaQueryWrapper<CompetitionCategory>()
                .eq(CompetitionCategory::getCompetitionId, competitionId));
        int sort = 0;
        for (ConfigNameItemRequest item : items) {
            competitionCategoryMapper.insert(CompetitionCategory.builder()
                    .competitionId(competitionId)
                    .name(item.getName())
                    .sortOrder(resolveSort(item.getSortOrder(), sort++))
                    .build());
        }
    }

    private void replaceEntryFields(Long competitionId, List<EntryFieldItemRequest> items) {
        entryFieldConfigMapper.delete(new LambdaQueryWrapper<EntryFieldConfig>()
                .eq(EntryFieldConfig::getCompetitionId, competitionId));
        int sort = 0;
        for (EntryFieldItemRequest item : items) {
            entryFieldConfigMapper.insert(EntryFieldConfig.builder()
                    .competitionId(competitionId)
                    .fieldKey(item.getFieldKey())
                    .fieldLabel(item.getFieldLabel())
                    .fieldType(item.getFieldType())
                    .helpText(normalizeNullable(item.getHelpText()))
                    .optionsJson(writeOptions(item.getOptions()))
                    .requiredFlag(Boolean.TRUE.equals(item.getRequired()) ? 1 : 0)
                    .visibleToJudges(Boolean.TRUE.equals(item.getVisibleToJudges()) ? 1 : 0)
                    .sortOrder(resolveSort(item.getSortOrder(), sort++))
                    .build());
        }
    }

    private void replaceScoreConfigs(Long competitionId, List<ScoreConfigItemRequest> configs) {
        competitionScoreConfigMapper.delete(new LambdaQueryWrapper<CompetitionScoreConfig>()
                .eq(CompetitionScoreConfig::getCompetitionId, competitionId));
        for (ScoreConfigItemRequest item : configs) {
            competitionScoreConfigMapper.insert(CompetitionScoreConfig.builder()
                    .competitionId(competitionId)
                    .judgeRoleType(item.getJudgeRoleType().name())
                    .minCommentLength(resolveMinCommentLength(item.getMinCommentLength()))
                    .dimensionsJson(writeDimensions(item.getDimensions()))
                    .build());
        }
    }

    private void validateCreateRequest(CompetitionCreateRequest request) {
        if (request.getRegistrationDeadline() != null
                && request.getRegistrationStart() != null
                && !request.getRegistrationDeadline().isAfter(request.getRegistrationStart())) {
            throw new BaseException("报名截止时间必须晚于报名开始时间");
        }
        validateEarlyBirdConfig(request.getEarlyBirdFee(), request.getEarlyBirdDeadline(),
                request.getEntryFee(), request.getRegistrationStart(), request.getRegistrationDeadline());
        CompetitionType.of(request.getCompetitionType());
        normalizeRulesUrl(request.getRulesUrl());
        if (!StringUtils.hasText(request.getStyleLibraryVersion())) {
            throw new BaseException("基础风格库不能为空");
        }
    }

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

    private String normalizeRulesUrl(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        if (!(normalized.startsWith("http://") || normalized.startsWith("https://"))) {
            throw new BaseException("参赛细则链接必须以 http:// 或 https:// 开头");
        }
        if (normalized.length() > 500) {
            throw new BaseException("参赛细则链接不能超过 500 个字符");
        }
        return normalized;
    }

    private void insertCompetitionWithGeneratedCode(Competition competition) {
        for (int i = 0; i < COMPETITION_CODE_RETRY_LIMIT; i++) {
            competition.setCode(generateCompetitionCode(competition.getCompetitionDate()));
            try {
                competitionMapper.insert(competition);
                return;
            } catch (DuplicateKeyException ex) {
                competition.setId(null);
            }
        }
        throw new BaseException("比赛编号生成失败，请重试");
    }

    private CompetitionDetailVO buildDetail(Competition competition) {
        Long competitionId = competition.getId();
        List<CompetitionConfigNameVO> categories = listCategories(competitionId);
        List<CompetitionConfigNameVO> styles = listStyles(competitionId);
        List<EntryFieldConfigVO> entryFields = listEntryFields(competitionId);
        List<JudgeTableVO> judgeTables = listJudgeTables(competitionId);
        List<ScoreConfigVO> scoreConfigs = listScoreConfigs(competitionId);
        EntrySummaryVO entriesSummary = buildEntriesSummary(competitionId);
        ProgressSummaryVO progressSummary = buildProgressSummary(competitionId, entriesSummary);
        ResultSetupVO resultSetup = buildResultSetup(competition);
        List<CompetitionCheckVO> checks = buildChecks(competition, categories, styles, entryFields, judgeTables, scoreConfigs,
                entriesSummary, resultSetup);
        List<CompetitionRoundVO> rounds = roundService.listCompetitionRounds(competitionId);
        List<CompetitionEntryVO> entryPool = roundService.listEntryPool(competitionId);
        List<ResultDraftVO> resultDrafts = roundService.buildResultDrafts(competitionId);
        List<AwardRuleVO> awardRules = awardService.listAwardRules(competitionId);
        List<AwardResultVO> awardResults = awardService.listAwardResults(competitionId);
        List<String> dataIntegrityIssues = buildDataIntegrityIssues(competition, checks);
        List<CompetitionStageCheckVO> stageChecks = buildStageChecks(competition, checks, dataIntegrityIssues);
        List<CompetitionAlertVO> alerts = buildAlerts(checks, entriesSummary, dataIntegrityIssues);
        CompetitionPrimaryActionVO primaryAction = buildPrimaryAction(competition, stageChecks, dataIntegrityIssues);

        return CompetitionDetailVO.builder()
                .id(competition.getId())
                .code(competition.getCode())
                .name(competition.getName())
                .competitionType(resolveCompetitionType(competition).name())
                .competitionDate(competition.getCompetitionDate())
                .registrationStart(competition.getRegistrationStart())
                .registrationDeadline(competition.getRegistrationDeadline())
                .status(competition.getStatus())
                .entryFee(competition.getEntryFee())
                .earlyBirdFee(competition.getEarlyBirdFee())
                .earlyBirdDeadline(competition.getEarlyBirdDeadline())
                .description(competition.getDescription())
                .rulesUrl(competition.getRulesUrl())
                .styleLibraryVersion(competition.getStyleLibraryVersion())
                .logistics(toCompetitionLogisticsVO(competition))
                .currentStageLabel(resolveStageLabel(parseStatus(competition)))
                .primaryAction(primaryAction)
                .categories(categories)
                .styles(styles)
                .entryFields(entryFields)
                .judgeTables(judgeTables)
                .scoreConfigs(scoreConfigs)
                .checks(checks)
                .stageChecks(stageChecks)
                .editableScopes(buildEditableScopes(competition))
                .entriesSummary(entriesSummary)
                .entries(entryPool)
                .entryPool(entryPool)
                .rounds(rounds)
                .currentRound(rounds.isEmpty() ? null : rounds.get(rounds.size() - 1))
                .resultDrafts(resultDrafts)
                .awardRules(awardRules)
                .awardResults(awardResults)
                .progressSummary(progressSummary)
                .resultSetup(resultSetup)
                .alerts(alerts)
                .dataIntegrityIssues(dataIntegrityIssues)
                .build();
    }

    private CompetitionVO toCompetitionVO(Competition competition, CompetitionDetailVO detail) {
        int readyCount = countDoneChecks(detail.getChecks());
        int judgeCount = detail.getJudgeTables().stream()
                .mapToInt(table -> table.getCaptainCount() + table.getProfessionalCount() + table.getCrossCount())
                .sum();
        return CompetitionVO.builder()
                .id(competition.getId())
                .code(competition.getCode())
                .name(competition.getName())
                .competitionType(resolveCompetitionType(competition).name())
                .competitionDate(competition.getCompetitionDate())
                .registrationStart(competition.getRegistrationStart())
                .registrationDeadline(competition.getRegistrationDeadline())
                .status(competition.getStatus())
                .entryFee(competition.getEntryFee())
                .earlyBirdFee(competition.getEarlyBirdFee())
                .earlyBirdDeadline(competition.getEarlyBirdDeadline())
                .description(competition.getDescription())
                .rulesUrl(competition.getRulesUrl())
                .styleLibraryVersion(competition.getStyleLibraryVersion())
                .logistics(toCompetitionLogisticsVO(competition))
                .currentStageLabel(detail.getCurrentStageLabel())
                .primaryAction(detail.getPrimaryAction())
                .readyCount(readyCount)
                .checkTotal(detail.getChecks().size())
                .alertCount(detail.getAlerts().size())
                .nextAction(resolveNextAction(parseStatus(competition)))
                .dataIntegrityIssues(detail.getDataIntegrityIssues())
                .entriesSummary(detail.getEntriesSummary())
                .progressSummary(detail.getProgressSummary())
                .judgeTableCount(detail.getJudgeTables().size())
                .judgeCount(judgeCount)
                .build();
    }

    private PortalCompetitionVO toPortalCompetitionVO(Competition competition) {
        return PortalCompetitionVO.builder()
                .id(competition.getId())
                .code(competition.getCode())
                .name(competition.getName())
                .competitionType(resolveCompetitionType(competition).name())
                .competitionDate(competition.getCompetitionDate())
                .registrationStart(competition.getRegistrationStart())
                .registrationDeadline(competition.getRegistrationDeadline())
                .status(competition.getStatus())
                .entryFee(competition.getEntryFee())
                .earlyBirdFee(competition.getEarlyBirdFee())
                .earlyBirdDeadline(competition.getEarlyBirdDeadline())
                .description(competition.getDescription())
                .rulesUrl(competition.getRulesUrl())
                .logistics(toPublicCompetitionLogisticsVO(competition))
                .currentStageLabel(resolveStageLabel(parseStatus(competition)))
                .categories(listCategories(competition.getId()))
                .styles(listStyles(competition.getId()))
                .entryFields(listEntryFields(competition.getId()))
                .build();
    }

    private void applyCreateLogistics(Competition competition, CompetitionCreateRequest request) {
        competition.setDeliveryMethod(normalizeDeliveryMethod(request.getDeliveryMethod()));
        competition.setSampleArrivalStart(request.getSampleArrivalStart());
        competition.setSampleArrivalDeadline(request.getSampleArrivalDeadline());
        competition.setSampleQuantityNote(normalizeNullable(request.getSampleQuantityNote()));
        competition.setDeliveryRecipient(normalizeNullable(request.getDeliveryRecipient()));
        competition.setDeliveryPhone(normalizeNullable(request.getDeliveryPhone()));
        competition.setDeliveryAddress(normalizeNullable(request.getDeliveryAddress()));
        competition.setDeliveryNote(normalizeNullable(request.getDeliveryNote()));
        competition.setLogisticsVisibility(normalizeLogisticsVisibility(request.getLogisticsVisibility()));
        validateLogisticsTime(competition);
    }

    private void applyBaseInfoLogistics(Competition competition, CompetitionBaseInfoUpdateRequest request) {
        competition.setDeliveryMethod(normalizeDeliveryMethod(request.getDeliveryMethod()));
        competition.setSampleArrivalStart(request.getSampleArrivalStart());
        competition.setSampleArrivalDeadline(request.getSampleArrivalDeadline());
        competition.setSampleQuantityNote(normalizeNullable(request.getSampleQuantityNote()));
        competition.setDeliveryRecipient(normalizeNullable(request.getDeliveryRecipient()));
        competition.setDeliveryPhone(normalizeNullable(request.getDeliveryPhone()));
        competition.setDeliveryAddress(normalizeNullable(request.getDeliveryAddress()));
        competition.setDeliveryNote(normalizeNullable(request.getDeliveryNote()));
        competition.setLogisticsVisibility(normalizeLogisticsVisibility(request.getLogisticsVisibility()));
        validateLogisticsTime(competition);
    }

    private CompetitionLogisticsVO toCompetitionLogisticsVO(Competition competition) {
        return CompetitionLogisticsVO.builder()
                .deliveryMethod(resolveDeliveryMethod(competition.getDeliveryMethod()))
                .sampleArrivalStart(competition.getSampleArrivalStart())
                .sampleArrivalDeadline(competition.getSampleArrivalDeadline())
                .sampleQuantityNote(competition.getSampleQuantityNote())
                .deliveryRecipient(competition.getDeliveryRecipient())
                .deliveryPhone(competition.getDeliveryPhone())
                .deliveryAddress(competition.getDeliveryAddress())
                .deliveryNote(competition.getDeliveryNote())
                .logisticsVisibility(resolveLogisticsVisibility(competition.getLogisticsVisibility()))
                .build();
    }

    private CompetitionLogisticsVO toPublicCompetitionLogisticsVO(Competition competition) {
        CompetitionLogisticsVO logistics = toCompetitionLogisticsVO(competition);
        if (LogisticsVisibility.PUBLIC.name().equals(logistics.getLogisticsVisibility())) {
            return logistics;
        }
        logistics.setDeliveryRecipient(null);
        logistics.setDeliveryPhone(null);
        logistics.setDeliveryAddress(null);
        return logistics;
    }

    private String normalizeDeliveryMethod(String deliveryMethod) {
        String normalized = normalizeNullable(deliveryMethod);
        if (!StringUtils.hasText(normalized)) {
            return DEFAULT_DELIVERY_METHOD;
        }
        try {
            return CompetitionDeliveryMethod.valueOf(normalized.toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            throw new BaseException("送样方式不正确");
        }
    }

    private String normalizeLogisticsVisibility(String logisticsVisibility) {
        String normalized = normalizeNullable(logisticsVisibility);
        if (!StringUtils.hasText(normalized)) {
            return DEFAULT_LOGISTICS_VISIBILITY;
        }
        try {
            return LogisticsVisibility.valueOf(normalized.toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            throw new BaseException("送样地址展示规则不正确");
        }
    }

    private String resolveDeliveryMethod(String deliveryMethod) {
        return StringUtils.hasText(deliveryMethod) ? deliveryMethod : DEFAULT_DELIVERY_METHOD;
    }

    private String resolveLogisticsVisibility(String logisticsVisibility) {
        return StringUtils.hasText(logisticsVisibility) ? logisticsVisibility : DEFAULT_LOGISTICS_VISIBILITY;
    }

    private void validateLogisticsTime(Competition competition) {
        if (competition.getSampleArrivalStart() != null
                && competition.getSampleArrivalDeadline() != null
                && !competition.getSampleArrivalDeadline().isAfter(competition.getSampleArrivalStart())) {
            throw new BaseException("送达截止时间必须晚于送达开始时间");
        }
    }

    private List<CompetitionConfigNameVO> listCategories(Long competitionId) {
        return competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionCategory::getSortOrder)
                        .orderByAsc(CompetitionCategory::getId))
                .stream()
                .map(item -> CompetitionConfigNameVO.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
    }

    private List<CompetitionConfigNameVO> listStyles(Long competitionId) {
        return competitionStyleConfigMapper.selectList(new LambdaQueryWrapper<CompetitionStyleConfig>()
                        .eq(CompetitionStyleConfig::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionStyleConfig::getSortOrder)
                        .orderByAsc(CompetitionStyleConfig::getId))
                .stream()
                .map(item -> CompetitionConfigNameVO.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .categoryName(item.getCategoryName())
                        .styleCode(item.getStyleCode())
                        .description(item.getDescription())
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
    }

    private List<EntryFieldConfigVO> listEntryFields(Long competitionId) {
        return entryFieldConfigMapper.selectList(new LambdaQueryWrapper<EntryFieldConfig>()
                        .eq(EntryFieldConfig::getCompetitionId, competitionId)
                        .orderByAsc(EntryFieldConfig::getSortOrder)
                        .orderByAsc(EntryFieldConfig::getId))
                .stream()
                .map(item -> EntryFieldConfigVO.builder()
                        .id(item.getId())
                        .fieldKey(item.getFieldKey())
                        .fieldLabel(item.getFieldLabel())
                        .fieldType(item.getFieldType())
                        .helpText(item.getHelpText())
                        .options(readOptions(item.getOptionsJson()))
                        .required(Objects.equals(item.getRequiredFlag(), 1))
                        .visibleToJudges(Objects.equals(item.getVisibleToJudges(), 1))
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
    }

    private List<JudgeTableVO> listJudgeTables(Long competitionId) {
        List<JudgeTable> tables = judgeTableMapper.selectList(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, competitionId)
                .orderByAsc(JudgeTable::getSortOrder)
                .orderByAsc(JudgeTable::getId));
        List<JudgeAssignment> assignments = judgeAssignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId));
        List<ScoreRecord> finalScores = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getCompetitionId, competitionId)
                .eq(ScoreRecord::getFinalFlag, 1));

        Map<Long, List<JudgeAssignment>> assignmentsByTable = assignments.stream()
                .collect(Collectors.groupingBy(JudgeAssignment::getTableId));
        Map<Long, Long> finalizedByTable = finalScores.stream()
                .filter(record -> record.getAssignmentId() != null)
                .collect(Collectors.groupingBy(ScoreRecord::getAssignmentId, Collectors.counting()));
        Map<Long, JudgeAssignment> assignmentById = assignments.stream()
                .collect(Collectors.toMap(JudgeAssignment::getId, Function.identity(), (left, right) -> left));
        Set<Long> judgeIds = assignments.stream()
                .map(JudgeAssignment::getJudgeAccountId)
                .collect(Collectors.toSet());
        Map<Long, JudgeAccount> judgeById = judgeIds.isEmpty()
                ? Map.of()
                : judgeAccountMapper.selectBatchIds(judgeIds)
                        .stream()
                        .collect(Collectors.toMap(JudgeAccount::getId, Function.identity()));

        return tables.stream()
                .map(table -> {
                    List<JudgeAssignment> tableAssignments = assignmentsByTable.getOrDefault(table.getId(), List.of());
                    int captain = countRole(tableAssignments, JudgeRoleType.CAPTAIN.name());
                    int professional = countRole(tableAssignments, JudgeRoleType.PROFESSIONAL.name());
                    int cross = countRole(tableAssignments, JudgeRoleType.CROSS.name());
                    int finalized = finalizedByTable.entrySet().stream()
                            .filter(entry -> {
                                JudgeAssignment assignment = assignmentById.get(entry.getKey());
                                return assignment != null && Objects.equals(assignment.getTableId(), table.getId());
                            })
                            .mapToInt(entry -> entry.getValue().intValue())
                            .sum();
                    return JudgeTableVO.builder()
                            .id(table.getId())
                            .tableName(table.getTableName())
                            .captainCount(captain)
                            .professionalCount(professional)
                            .crossCount(cross)
                            .finalized(finalized)
                            .total(0)
                            .assignments(tableAssignments.stream()
                                    .map(assignment -> JudgeAssignmentVO.builder()
                                            .id(assignment.getId())
                                            .tableId(assignment.getTableId())
                                            .judgePublicId(judgeById.get(assignment.getJudgeAccountId()) == null
                                                    ? null
                                                    : judgeById.get(assignment.getJudgeAccountId()).getPublicId())
                                            .role(assignment.getRole())
                                            .build())
                                    .toList())
                            .build();
                })
                .toList();
    }

    private List<ScoreConfigVO> listScoreConfigs(Long competitionId) {
        return competitionScoreConfigMapper.selectList(new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionScoreConfig::getId))
                .stream()
                .map(this::toScoreConfigVO)
                .toList();
    }

    private EntrySummaryVO buildEntriesSummary(Long competitionId) {
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId));
        int total = entries.size();
        int pendingPayment = countEntryStatus(entries, "PENDING_PAYMENT");
        int canceled = countEntryStatus(entries, "CANCELED");
        int stored = (int) entries.stream().filter(entry -> Objects.equals(entry.getStoredFlag(), 1)).count();
        int resultPublished = countEntryStatus(entries, "RESULT_PUBLISHED") + countEntryStatus(entries, "PUBLISHED");
        int registered = Math.max(0, total - canceled);
        return EntrySummaryVO.builder()
                .total(total)
                .pendingPayment(pendingPayment)
                .registered(registered)
                .stored(stored)
                .canceled(canceled)
                .resultPublished(resultPublished)
                .build();
    }

    private List<CompetitionEntryVO> listEntries(Long competitionId, List<CompetitionConfigNameVO> categories) {
        Map<Long, String> categoryNameById = categories.stream()
                .collect(Collectors.toMap(CompetitionConfigNameVO::getId, CompetitionConfigNameVO::getName));
        Map<String, CompetitionConfigNameVO> styleByName = listStyles(competitionId).stream()
                .collect(Collectors.toMap(CompetitionConfigNameVO::getName, Function.identity(), (left, right) -> left));
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                        .eq(BeerEntry::getCompetitionId, competitionId)
                        .orderByDesc(BeerEntry::getCreateTime)
                        .last("LIMIT 20"));
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entries.stream()
                .map(BeerEntry::getId)
                .toList());
        Map<Long, EntryRefund> refundByEntryId = loadLatestRefunds(entries.stream()
                .map(BeerEntry::getId)
                .collect(Collectors.toSet()));
        Map<Long, EntryDelivery> deliveryByEntryId = loadEntryDeliveries(entries.stream()
                .map(BeerEntry::getId)
                .collect(Collectors.toSet()));
        return entries.stream()
                .map(entry -> {
                    CompetitionConfigNameVO style = styleByName.get(entry.getStyle());
                    EntryScanLabel label = labelByEntryId.get(entry.getId());
                    EntryRefund refund = refundByEntryId.get(entry.getId());
                    EntryDelivery delivery = deliveryByEntryId.get(entry.getId());
                    return CompetitionEntryVO.builder()
                            .id(entry.getId())
                            .uuid(entry.getUuid())
                            .labelCode(label == null ? null : label.getLabelCode())
                            .shortCode(label == null ? null : label.getShortCode())
                            .scanToken(label == null ? null : label.getScanToken())
                            .categoryName(categoryNameById.getOrDefault(entry.getCategoryId(), "-"))
                            .style(entry.getStyle())
                            .styleCategoryName(style == null ? null : style.getCategoryName())
                            .styleCode(style == null ? null : style.getStyleCode())
                            .styleDescription(style == null ? null : style.getDescription())
                            .status(entry.getStatus())
                            .refundStatus(refund == null ? null : refund.getStatus())
                            .refundReason(refund == null ? null : refund.getReason())
                            .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                            .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                            .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                            .deliveryStatus(delivery == null ? null : delivery.getDeliveryStatus())
                            .carrier(delivery == null ? null : delivery.getCarrier())
                            .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                            .deliverySubmittedAt(delivery == null ? null : delivery.getSubmittedTime())
                            .stored(Objects.equals(entry.getStoredFlag(), 1))
                            .build();
                })
                .toList();
    }

    private Map<Long, EntryDelivery> loadEntryDeliveries(Set<Long> entryIds) {
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

    private ProgressSummaryVO buildProgressSummary(Long competitionId, EntrySummaryVO entriesSummary) {
        CompetitionRound currentRound = competitionRoundMapper.selectList(new LambdaQueryWrapper<CompetitionRound>()
                        .eq(CompetitionRound::getCompetitionId, competitionId)
                        .orderByDesc(CompetitionRound::getSortOrder)
                        .orderByDesc(CompetitionRound::getId))
                .stream()
                .filter(round -> !RoundStatus.DRAFT.name().equals(round.getStatus()))
                .findFirst()
                .orElse(null);
        if (currentRound == null) {
            List<ScoreRecord> finalScores = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                    .eq(ScoreRecord::getCompetitionId, competitionId)
                    .eq(ScoreRecord::getFinalFlag, 1));
            int advanced = (int) finalScores.stream()
                    .filter(record -> Objects.equals(record.getAdvancedFlag(), 1))
                    .count();
            return ProgressSummaryVO.builder()
                    .finalized(finalScores.size())
                    .total(entriesSummary.getRegistered())
                    .advanced(advanced)
                    .commentWarnings(0)
                    .averageReviewMinutes(0)
                    .averageReviewSeconds(0)
                    .averageCommentChars(0)
                    .build();
        }
        if (RoundType.RANKING.name().equals(currentRound.getRoundType())) {
            List<RoundTable> tables = roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                    .eq(RoundTable::getRoundId, currentRound.getId()));
            int submittedTables = (int) tables.stream()
                    .filter(table -> RoundStatus.SUBMITTED.name().equals(table.getStatus()) || RoundStatus.LOCKED.name().equals(table.getStatus()))
                    .count();
            int results = Math.toIntExact(roundResultMapper.selectCount(new LambdaQueryWrapper<RoundResult>()
                    .eq(RoundResult::getRoundId, currentRound.getId())));
            List<RoundResult> roundResults = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                    .eq(RoundResult::getRoundId, currentRound.getId()));
            return ProgressSummaryVO.builder()
                    .finalized(submittedTables)
                    .total(tables.size())
                    .advanced(results)
                    .commentWarnings(0)
                    .averageReviewMinutes(averageMinutes(currentRound.getPublishedTime(),
                            roundResults.stream().map(RoundResult::getSubmittedTime).toList()))
                    .averageReviewSeconds(0)
                    .averageCommentChars(0)
                    .build();
        }
        List<RoundTableEntry> roundEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, currentRound.getId()));
        Set<Long> entryIds = roundEntries.stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        List<ScoreRecord> roundScores = entryIds.isEmpty()
                ? List.of()
                : scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, competitionId)
                        .in(ScoreRecord::getBeerEntryId, entryIds));
        List<ScoreRecord> finalScores = roundScores.stream()
                .filter(record -> Objects.equals(record.getFinalFlag(), 1))
                .toList();
        List<ScoreRecord> judgeScores = roundScores.stream()
                .filter(record -> Objects.equals(record.getRoundId(), currentRound.getId()))
                .filter(record -> Objects.equals(record.getFinalFlag(), 0))
                .toList();
        int advanced = (int) finalScores.stream()
                .filter(record -> Objects.equals(record.getAdvancedFlag(), 1))
                .count();
        int captainMinCommentLength = competitionScoreConfigMapper.selectList(new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId)
                        .eq(CompetitionScoreConfig::getJudgeRoleType, JudgeRoleType.CAPTAIN.name()))
                .stream()
                .findFirst()
                .map(CompetitionScoreConfig::getMinCommentLength)
                .orElse(DEFAULT_MIN_COMMENT_LENGTH);
        int commentWarnings = captainMinCommentLength <= 0
                ? 0
                : (int) finalScores.stream()
                        .filter(record -> String.valueOf(record.getComments() == null ? "" : record.getComments()).trim().length() < captainMinCommentLength)
                        .count();
        return ProgressSummaryVO.builder()
                .finalized(finalScores.size())
                .total(roundEntries.size())
                .advanced(advanced)
                .commentWarnings(commentWarnings)
                .averageReviewMinutes(averageMinutes(currentRound.getPublishedTime(),
                        roundScores.stream().map(ScoreRecord::getCreateTime).toList()))
                .averageReviewSeconds(averageInt(judgeScores.stream()
                        .map(ScoreRecord::getDurationSeconds)
                        .filter(Objects::nonNull)
                        .toList()))
                .averageCommentChars(averageInt(judgeScores.stream()
                        .map(ScoreRecord::getCommentCharCount)
                        .filter(Objects::nonNull)
                        .toList()))
                .build();
    }

    private int averageInt(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private int averageMinutes(LocalDateTime startTime, List<LocalDateTime> endTimes) {
        if (startTime == null || endTimes == null || endTimes.isEmpty()) {
            return 0;
        }
        List<Long> minutes = endTimes.stream()
                .filter(Objects::nonNull)
                .filter(endTime -> !endTime.isBefore(startTime))
                .map(endTime -> Math.max(0L, Duration.between(startTime, endTime).toMinutes()))
                .toList();
        if (minutes.isEmpty()) {
            return 0;
        }
        return Math.toIntExact(Math.round(minutes.stream().mapToLong(Long::longValue).average().orElse(0)));
    }

    private ResultSetupVO buildResultSetup(Competition competition) {
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

    private List<CompetitionCheckVO> buildChecks(Competition competition,
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
        checks.add(check("storedEntries", "酒款入库", entriesSummary.getRegistered() > 0
                && entriesSummary.getStored() >= entriesSummary.getRegistered(), "报名酒款需要完成入库确认"));
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

    private List<String> buildDataIntegrityIssues(Competition competition, List<CompetitionCheckVO> checks) {
        if (parseStatus(competition) == CompetitionStatus.DRAFT) {
            return List.of();
        }
        return checks.stream()
                .filter(check -> isBlockingCheck(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .map(check -> "比赛已进入" + resolveStageLabel(parseStatus(competition)) + "，但" + check.getLabel() + "未完成")
                .toList();
    }

    private List<CompetitionStageCheckVO> buildStageChecks(Competition competition,
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

    private List<CompetitionAlertVO> buildAlerts(List<CompetitionCheckVO> checks,
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

    private CompetitionPrimaryActionVO buildPrimaryAction(Competition competition,
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

    private Map<String, Boolean> buildEditableScopes(Competition competition) {
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
        scopes.put("styleLibrary", draft);
        scopes.put("styles", draft);
        scopes.put("entryFields", draft);
        scopes.put("judgeConfig", judgeConfig);
        scopes.put("judgeTables", judgeConfig);
        scopes.put("scoreConfigs", judgeConfig);
        scopes.put("resultConfig", status == CompetitionStatus.RESULT_CONFIRMING);
        return scopes;
    }

    private void assertBaseInfoEditable(Competition competition, CompetitionBaseInfoUpdateRequest request) {
        CompetitionStatus status = parseStatus(competition);
        if (status == CompetitionStatus.DRAFT) {
            return;
        }
        if (status != CompetitionStatus.REGISTRATION_OPEN && status != CompetitionStatus.REGISTRATION_CLOSED) {
            if (onlyDescriptionChangedOrUnchanged(competition, request)) {
                return;
            }
            throw new BaseException("当前阶段基础信息已锁定");
        }
        if (!Objects.equals(competition.getName(), request.getName())
                || !Objects.equals(competition.getCode(), request.getCode())
                || !Objects.equals(competition.getRegistrationStart(), request.getRegistrationStart())) {
            throw new BaseException("报名已开放，仅允许修改比赛日期、报名截止时间、费用、简介和送样信息");
        }
        if (competition.getEntryFee().compareTo(request.getEntryFee()) != 0 && hasEntries(competition.getId())) {
            throw new BaseException("已有报名酒款，报名费已锁定");
        }
    }

    private boolean onlyDescriptionChangedOrUnchanged(Competition competition, CompetitionBaseInfoUpdateRequest request) {
        return Objects.equals(competition.getName(), request.getName())
                && Objects.equals(competition.getCode(), request.getCode())
                && Objects.equals(competition.getCompetitionDate(), request.getCompetitionDate())
                && Objects.equals(competition.getRegistrationStart(), request.getRegistrationStart())
                && Objects.equals(competition.getRegistrationDeadline(), request.getRegistrationDeadline())
                && compareBigDecimal(competition.getEntryFee(), request.getEntryFee())
                && compareBigDecimal(competition.getEarlyBirdFee(), request.getEarlyBirdFee())
                && Objects.equals(competition.getEarlyBirdDeadline(), request.getEarlyBirdDeadline())
                && Objects.equals(competition.getDeliveryMethod(), normalizeDeliveryMethod(request.getDeliveryMethod()))
                && Objects.equals(competition.getSampleArrivalStart(), request.getSampleArrivalStart())
                && Objects.equals(competition.getSampleArrivalDeadline(), request.getSampleArrivalDeadline())
                && Objects.equals(competition.getSampleQuantityNote(), normalizeNullable(request.getSampleQuantityNote()))
                && Objects.equals(competition.getDeliveryRecipient(), normalizeNullable(request.getDeliveryRecipient()))
                && Objects.equals(competition.getDeliveryPhone(), normalizeNullable(request.getDeliveryPhone()))
                && Objects.equals(competition.getDeliveryAddress(), normalizeNullable(request.getDeliveryAddress()))
                && Objects.equals(competition.getDeliveryNote(), normalizeNullable(request.getDeliveryNote()))
                && Objects.equals(competition.getLogisticsVisibility(), normalizeLogisticsVisibility(request.getLogisticsVisibility()));
    }

    private boolean compareBigDecimal(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private void assertEntryStructureEditable(Competition competition) {
        if (parseStatus(competition) != CompetitionStatus.DRAFT) {
            throw new BaseException("报名已开放，报名结构已锁定");
        }
    }

    private void assertJudgeConfigEditable(Competition competition) {
        CompetitionStatus status = parseStatus(competition);
        if (status == CompetitionStatus.DRAFT
                || status == CompetitionStatus.REGISTRATION_OPEN
                || status == CompetitionStatus.REGISTRATION_CLOSED
                || status == CompetitionStatus.JUDGING_PREP) {
            return;
        }
        throw new BaseException("评审已开始，评审桌和评分表已锁定");
    }

    private void validateScoreConfigs(List<ScoreConfigItemRequest> configs) {
        Set<JudgeRoleType> roles = configs.stream()
                .map(ScoreConfigItemRequest::getJudgeRoleType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(JudgeRoleType.class)));
        if (!roles.equals(EnumSet.allOf(JudgeRoleType.class)) || configs.size() != JudgeRoleType.values().length) {
            throw new BaseException("评分表需要同时配置跨界评审、专业评审和桌长");
        }
        for (ScoreConfigItemRequest config : configs) {
            config.setMinCommentLength(resolveMinCommentLength(config.getMinCommentLength()));
            config.getDimensions().forEach(dimension -> {
                dimension.setKey(normalizeRequired(dimension.getKey(), "评分维度 key 不能为空"));
                dimension.setLabel(normalizeRequired(dimension.getLabel(), "评分维度名称不能为空"));
                dimension.setNotePrompt(normalizeNullable(dimension.getNotePrompt()));
            });
            if (config.getJudgeRoleType() == JudgeRoleType.CROSS
                    && (config.getDimensions().size() < CROSS_MIN_DIMENSIONS || config.getDimensions().size() > CROSS_MAX_DIMENSIONS)) {
                throw new BaseException("跨界评审需配置 2-3 个维度");
            }
            if (config.getJudgeRoleType() == JudgeRoleType.PROFESSIONAL) {
                validateProfessionalScoreDimensions(config.getDimensions());
            }
            if (config.getJudgeRoleType() == JudgeRoleType.CAPTAIN && config.getDimensions().size() != 1) {
                throw new BaseException("桌长评分表只能配置 1 个共识评分维度");
            }
            BigDecimal total = config.getDimensions().stream()
                    .map(DimensionRequest::getMaxScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(SCORE_FORM_TOTAL) != 0) {
                throw new BaseException("评分表总分必须等于 50 分：" + config.getJudgeRoleType().name());
            }
            assertUniqueKeys(config.getDimensions().stream().map(DimensionRequest::getKey).toList(), "评分维度 key");
            assertUniqueKeys(config.getDimensions().stream().map(DimensionRequest::getLabel).toList(), "评分维度名称");
        }
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

    private void validateProfessionalScoreDimensions(List<DimensionRequest> dimensions) {
        if (dimensions.size() != PROFESSIONAL_DIMENSION_LABELS.size()) {
            throw new BaseException("专业评审评分表需固定为香气、外观、味道、口感、整体印象 5 个维度");
        }
        for (int index = 0; index < PROFESSIONAL_DIMENSION_LABELS.size(); index++) {
            DimensionRequest dimension = dimensions.get(index);
            String expectedLabel = PROFESSIONAL_DIMENSION_LABELS.get(index);
            BigDecimal expectedScore = PROFESSIONAL_DIMENSION_MAX_SCORES.get(index);
            if (!expectedLabel.equals(dimension.getLabel()) || dimension.getMaxScore() == null
                    || dimension.getMaxScore().compareTo(expectedScore) != 0) {
                throw new BaseException("专业评审评分表需固定为香气12、外观3、味道20、口感5、整体印象10");
            }
        }
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

    private void assertChecksDone(List<CompetitionCheckVO> checks, Set<String> requiredKeys, String actionName) {
        List<String> missing = checks.stream()
                .filter(check -> requiredKeys.contains(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .map(CompetitionCheckVO::getLabel)
                .toList();
        if (!missing.isEmpty()) {
            throw new BaseException(actionName + "前还需要完成：" + String.join("、", missing));
        }
    }

    private void validateReopenRegistrationDeadline(Competition competition, LocalDateTime deadline, LocalDateTime now) {
        if (deadline == null) {
            return;
        }
        if (competition.getRegistrationStart() != null && !deadline.isAfter(competition.getRegistrationStart())) {
            throw new BaseException("新的报名截止时间必须晚于报名开始时间");
        }
        if (!deadline.isAfter(now)) {
            throw new BaseException("新的报名截止时间必须晚于当前时间");
        }
    }

    private void writeCompetitionStageLog(String action,
                                          Long competitionId,
                                          CompetitionStatus fromStatus,
                                          CompetitionStatus toStatus,
                                          String actionLabel,
                                          String reason,
                                          LocalDateTime oldDeadline,
                                          LocalDateTime newDeadline) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", actionLabel);
        payload.put("fromStatus", fromStatus.name());
        payload.put("toStatus", toStatus.name());
        payload.put("reason", truncateNullable(reason, 120));
        payload.put("oldRegistrationDeadline", oldDeadline == null ? null : oldDeadline.format(EXPORT_TIME_FORMAT));
        payload.put("newRegistrationDeadline", newDeadline == null ? null : newDeadline.format(EXPORT_TIME_FORMAT));
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(BaseContext.getCurrentId())
                .action(action)
                .targetType(LOG_TARGET_COMPETITION)
                .targetPublicId(String.valueOf(competitionId))
                .summary(writeObjectJson(payload, "保存比赛阶段操作日志失败"))
                .createTime(LocalDateTime.now())
                .build());
    }

    private String writeObjectJson(Object payload, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BaseException(errorMessage);
        }
    }

    private String truncateNullable(String value, int maxLength) {
        String normalized = normalizeNullable(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private BigDecimal getScoreTotal(List<DimensionRequest> dimensions) {
        return dimensions.stream()
                .map(DimensionRequest::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ConfigNameItemRequest> normalizeNameItems(List<ConfigNameItemRequest> items, String label) {
        List<ConfigNameItemRequest> normalized = items.stream()
                .peek(item -> item.setName(normalizeRequired(item.getName(), label + "名称不能为空")))
                .sorted(Comparator.comparing(item -> resolveSort(item.getSortOrder(), 0)))
                .toList();
        assertUniqueKeys(normalized.stream().map(ConfigNameItemRequest::getName).toList(), label + "名称");
        return normalized;
    }

    private List<EntryFieldItemRequest> normalizeEntryFields(List<EntryFieldItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<EntryFieldItemRequest> normalized = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            EntryFieldItemRequest item = items.get(index);
            item.setFieldLabel(normalizeRequired(item.getFieldLabel(), "字段名称不能为空"));
            item.setFieldType(normalizeRequired(item.getFieldType(), "字段类型不能为空"));
            if (!ENTRY_FIELD_TYPES.contains(item.getFieldType())) {
                throw new BaseException("字段类型不支持：" + item.getFieldType());
            }
            if (!StringUtils.hasText(item.getFieldKey())) {
                item.setFieldKey("custom_" + (index + 1));
            } else {
                item.setFieldKey(normalizeRequired(item.getFieldKey(), "字段 key 不能为空"));
            }
            item.setHelpText(normalizeNullable(item.getHelpText()));
            item.setOptions(normalizeOptions(item.getOptions()));
            normalized.add(item);
        }
        normalized = normalized.stream()
                .sorted(Comparator.comparing(item -> resolveSort(item.getSortOrder(), 0)))
                .toList();
        assertUniqueKeys(normalized.stream().map(EntryFieldItemRequest::getFieldKey).toList(), "报名字段 key");
        assertUniqueKeys(normalized.stream().map(EntryFieldItemRequest::getFieldLabel).toList(), "报名字段名称");
        return normalized;
    }

    private List<JudgeTableItemRequest> normalizeJudgeTables(List<JudgeTableItemRequest> items) {
        List<JudgeTableItemRequest> normalized = items.stream()
                .peek(item -> item.setTableName(normalizeRequired(item.getTableName(), "评审桌名称不能为空")))
                .toList();
        assertUniqueKeys(normalized.stream().map(JudgeTableItemRequest::getTableName).toList(), "评审桌名称");
        return normalized;
    }

    private void assertUniqueKeys(List<String> values, String label) {
        Set<String> uniqueValues = new LinkedHashSet<>();
        for (String value : values) {
            if (!uniqueValues.add(value)) {
                throw new BaseException(label + "不能重复：" + value);
            }
        }
    }

    private void assertCompetitionCodeUnique(String code, Long currentId) {
        if (competitionMapper.countByCodeIncludingDeleted(code, currentId) > 0) {
            throw new BaseException("比赛编号已存在");
        }
    }

    private Competition getCompetitionOrThrow(Long id) {
        Competition competition = competitionMapper.selectById(id);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private int closeExpiredRegistrations(LocalDateTime now) {
        if (now == null) {
            return 0;
        }
        return competitionMapper.update(null, new LambdaUpdateWrapper<Competition>()
                .set(Competition::getStatus, CompetitionStatus.REGISTRATION_CLOSED.name())
                .eq(Competition::getStatus, CompetitionStatus.REGISTRATION_OPEN.name())
                .eq(Competition::getDeletedFlag, FLAG_FALSE)
                .isNotNull(Competition::getRegistrationDeadline)
                .le(Competition::getRegistrationDeadline, now));
    }

    private CompetitionStatus parseStatus(Competition competition) {
        try {
            return CompetitionStatus.valueOf(competition.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BaseException("比赛状态不合法：" + competition.getStatus());
        }
    }

    private CompetitionType resolveCompetitionType(Competition competition) {
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

    private boolean hasEntries(Long competitionId) {
        return beerEntryMapper.selectCount(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)) > 0;
    }

    private boolean isBlockingCheck(String key) {
        return Set.of("baseInfo", "categories", "styleLibrary").contains(key);
    }

    private int countDoneChecks(List<CompetitionCheckVO> checks) {
        return (int) checks.stream()
                .filter(check -> CHECK_DONE.equals(check.getState()))
                .count();
    }

    private int countRole(List<JudgeAssignment> assignments, String role) {
        return (int) assignments.stream()
                .filter(assignment -> role.equals(assignment.getRole()))
                .count();
    }

    private int countEntryStatus(List<BeerEntry> entries, String status) {
        return (int) entries.stream()
                .filter(entry -> status.equals(entry.getStatus()))
                .count();
    }

    private int resolveSort(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private List<String> normalizeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        List<String> normalized = options.stream()
                .map(this::normalizeNullable)
                .filter(Objects::nonNull)
                .toList();
        assertUniqueKeys(normalized, "字段选项");
        return normalized;
    }

    private Integer resolveMinCommentLength(Integer minCommentLength) {
        return minCommentLength == null ? DEFAULT_MIN_COMMENT_LENGTH : minCommentLength;
    }

    private String generateCompetitionCode(LocalDate competitionDate) {
        String datePart = competitionDate.format(COMPETITION_CODE_DATE_FORMAT);
        String suffix = Integer.toString(ThreadLocalRandom.current().nextInt(CODE_SUFFIX_BOUND), 36)
                .toUpperCase();
        return COMPETITION_CODE_PREFIX + "-" + datePart + "-" + "0".repeat(5 - suffix.length()) + suffix;
    }

    private String resolveNextAction(CompetitionStatus status) {
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

    private String resolveStageLabel(CompetitionStatus status) {
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

    private ScoreConfigVO toScoreConfigVO(CompetitionScoreConfig config) {
        return ScoreConfigVO.builder()
                .competitionId(config.getCompetitionId())
                .judgeRoleType(config.getJudgeRoleType())
                .minCommentLength(resolveMinCommentLength(config.getMinCommentLength()))
                .dimensions(readDimensions(config.getDimensionsJson()))
                .build();
    }

    private List<DimensionRequest> readDimensions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<DimensionRequest>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BaseException("解析评分维度失败");
        }
    }

    private String writeDimensions(List<DimensionRequest> dimensions) {
        try {
            return objectMapper.writeValueAsString(dimensions);
        } catch (JsonProcessingException ex) {
            throw new BaseException("保存评分维度失败");
        }
    }

    private List<String> readOptions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BaseException("解析报名字段选项失败");
        }
    }

    private String writeOptions(List<String> options) {
        List<String> normalized = normalizeOptions(options);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw new BaseException("保存报名字段选项失败");
        }
    }
}
