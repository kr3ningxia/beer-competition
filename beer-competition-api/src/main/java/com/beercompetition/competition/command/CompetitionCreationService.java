package com.beercompetition.competition.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.CompetitionFeeTierMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.ConfigNameItemRequest;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.EntryFieldItemRequest;
import com.beercompetition.pojo.dto.ScoreConfigItemRequest;
import com.beercompetition.pojo.enums.CompetitionDeliveryMethod;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.LogisticsVisibility;
import com.beercompetition.pojo.enums.RefundApprovalMode;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.CompetitionFeeTier;
import com.beercompetition.pojo.dto.CompetitionFeeTierRequest;
import com.beercompetition.pojo.vo.CompetitionFeeTierVO;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.vo.CompetitionCheckVO;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionLogisticsVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.StyleItemVO;
import com.beercompetition.service.StyleLibraryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.beercompetition.competition.query.CompetitionQueryService;
import com.beercompetition.competition.access.CompetitionAccessService;

/**
 * 在一个事务中创建赛事主记录及其初始配置快照。
 */
@Service
@RequiredArgsConstructor
public class CompetitionCreationService {

    private static final BigDecimal SCORE_FORM_TOTAL = BigDecimal.valueOf(50);

    private static final String CHECK_DONE = "done";

    private static final int FLAG_FALSE = 0;

    private static final int FLAG_TRUE = 1;

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

    private static final DateTimeFormatter COMPETITION_CODE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private static final Set<String> ENTRY_FIELD_TYPES = Set.of("text", "textarea", "number", "select", "multi_select");

    private static final Set<String> OPTION_ENTRY_FIELD_TYPES = Set.of("select", "multi_select");

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;

    private final EntryFieldConfigMapper entryFieldConfigMapper;

    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;

    private final CompetitionFeeTierMapper competitionFeeTierMapper;

    private final StyleLibraryService styleLibraryService;

    private final ObjectMapper objectMapper;

    private final CompetitionQueryService competitionQueryService;

    private final CompetitionAccessService competitionAccessService;

    @Transactional(rollbackFor = Exception.class)
    public CompetitionVO createCompetition(CompetitionCreateRequest request) {
        // 1) 参数规范化与完整性校验
        validateCreateRequest(request);
        List<EntryFieldItemRequest> entryFields = normalizeEntryFields(request.getEntryFields() == null ? List.of() : request.getEntryFields());
        String styleLibraryVersion = normalizeRequired(request.getStyleLibraryVersion(), "基础风格库不能为空");
        List<StyleItemVO> snapshotStyles = styleLibraryService.listEnabledStyles(styleLibraryVersion);
        List<ConfigNameItemRequest> categories = normalizeNameItems(
                request.getCategories() == null ? List.of() : request.getCategories(), "投递组别");
        validateScoreConfigs(request.getScoreConfigs());
        validateFeeTiers(request.getTierPricingEnabled(), request.getFeeTiers());

        // 2) 构造草稿比赛主记录
        Competition competition = Competition.builder()
                .organizerId(competitionAccessService.requireCurrentOrganizerId())
                .name(normalizeRequired(request.getName(), "比赛名称不能为空"))
                .competitionDate(request.getCompetitionDate())
                .registrationStart(request.getRegistrationStart())
                .registrationDeadline(request.getRegistrationDeadline())
                .status(CompetitionStatus.DRAFT.name())
                .competitionType(CompetitionType.of(request.getCompetitionType()).name())
                .entryFee(request.getEntryFee())
                .earlyBirdFee(request.getEarlyBirdFee())
                .earlyBirdDeadline(request.getEarlyBirdDeadline())
                .tierPricingEnabled(Boolean.TRUE.equals(request.getTierPricingEnabled()) ? FLAG_TRUE : FLAG_FALSE)
                .refundApprovalMode(RefundApprovalMode.of(request.getRefundApprovalMode()).name())
                .description(normalizeRequired(request.getDescription(), "赛事简介不能为空"))
                .rulesUrl(normalizeRulesUrl(request.getRulesUrl()))
                .styleLibraryVersion(styleLibraryVersion)
                .build();
        applyCreateLogistics(competition, request);

        // 3) 事务内写入比赛和新建页完整配置
        insertCompetitionWithGeneratedCode(competition);
        replaceCategories(competition.getId(), categories);
        replaceStyleSnapshot(competition.getId(), styleLibraryVersion, snapshotStyles);
        replaceEntryFields(competition.getId(), entryFields);
        replaceScoreConfigs(competition.getId(), request.getScoreConfigs());
        replaceFeeTiers(competition.getId(), request.getTierPricingEnabled(), request.getFeeTiers());

        // 4) 返回列表口径摘要
        Competition saved = competitionMapper.selectById(competition.getId());
        return toCompetitionVO(saved, competitionQueryService.getCompetitionOverview(saved.getId()));
    }

    public void replaceFeeTiers(Long competitionId, Boolean enabled, List<CompetitionFeeTierRequest> requests) {
        competitionFeeTierMapper.delete(new LambdaQueryWrapper<CompetitionFeeTier>()
                .eq(CompetitionFeeTier::getCompetitionId, competitionId));
        if (!Boolean.TRUE.equals(enabled)) {
            return;
        }
        int sort = 0;
        for (CompetitionFeeTierRequest item : requests == null ? List.<CompetitionFeeTierRequest>of() : requests) {
            competitionFeeTierMapper.insert(CompetitionFeeTier.builder()
                    .competitionId(competitionId)
                    .startQuantity(item.getStartQuantity())
                    .discountRate(item.getDiscountRate())
                    .sortOrder(sort++)
                    .enabled(FLAG_TRUE)
                    .build());
        }
    }

    public void validateFeeTiers(Boolean enabled, List<CompetitionFeeTierRequest> requests) {
        if (!Boolean.TRUE.equals(enabled)) {
            return;
        }
        if (requests == null || requests.isEmpty()) {
            throw new BaseException("启用阶梯报名价后至少配置一档优惠");
        }
        int previousStart = 1;
        BigDecimal previousRate = BigDecimal.ONE;
        Set<Integer> starts = new LinkedHashSet<>();
        for (CompetitionFeeTierRequest item : requests) {
            if (item == null || item.getStartQuantity() == null || item.getDiscountRate() == null) {
                throw new BaseException("阶梯价格配置不完整");
            }
            if (item.getStartQuantity() <= 1 || item.getStartQuantity() <= previousStart || !starts.add(item.getStartQuantity())) {
                throw new BaseException("阶梯起始数量必须从第 2 款起按升序配置，且不能重复");
            }
            if (item.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0 || item.getDiscountRate().compareTo(BigDecimal.ONE) > 0) {
                throw new BaseException("阶梯折扣必须在 0.01 到 1.00 之间");
            }
            if (item.getDiscountRate().compareTo(previousRate) > 0) {
                throw new BaseException("后续阶梯折扣不能高于前一档");
            }
            previousStart = item.getStartQuantity();
            previousRate = item.getDiscountRate();
        }
    }

    private void replaceStyleSnapshot(Long competitionId, List<StyleItemVO> styles) {
        replaceStyleSnapshot(competitionId, null, styles);
    }

    private void replaceStyleSnapshot(Long competitionId, String sourceLibraryVersion, List<StyleItemVO> styles) {
        // 1) 停用旧快照，已报名酒款仍可通过 styleConfigId 读取历史说明
        competitionStyleConfigMapper.update(null, new LambdaUpdateWrapper<CompetitionStyleConfig>()
                .set(CompetitionStyleConfig::getActiveFlag, FLAG_FALSE)
                .eq(CompetitionStyleConfig::getCompetitionId, competitionId)
                .eq(CompetitionStyleConfig::getActiveFlag, FLAG_TRUE));

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
                    .activeFlag(FLAG_TRUE)
                    .sourceLibraryVersion(sourceLibraryVersion)
                    .build());
        }
    }

    private String categoryName(BeerEntry entry, Map<Long, CompetitionCategory> categoryById) {
        CompetitionCategory category = categoryById.get(entry.getCategoryId());
        return category == null ? "" : firstText(category.getName(), "");
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : (fallback == null ? "" : fallback);
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
                    .activeFlag(FLAG_TRUE)
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
                .tierPricingEnabled(Integer.valueOf(1).equals(competition.getTierPricingEnabled()))
                .feeTiers(listFeeTiers(competition.getId()))
                .refundApprovalMode(resolveRefundApprovalMode(competition).name())
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

    private List<CompetitionFeeTierVO> listFeeTiers(Long competitionId) {
        return competitionFeeTierMapper.selectList(new LambdaQueryWrapper<CompetitionFeeTier>()
                        .eq(CompetitionFeeTier::getCompetitionId, competitionId)
                        .eq(CompetitionFeeTier::getEnabled, FLAG_TRUE)
                        .orderByAsc(CompetitionFeeTier::getStartQuantity))
                .stream()
                .map(item -> CompetitionFeeTierVO.builder().id(item.getId()).startQuantity(item.getStartQuantity())
                        .discountRate(item.getDiscountRate()).sortOrder(item.getSortOrder()).enabled(item.getEnabled()).build())
                .toList();
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

    private RefundApprovalMode resolveRefundApprovalMode(Competition competition) {
        return RefundApprovalMode.of(competition == null ? null : competition.getRefundApprovalMode());
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

    private List<ConfigNameItemRequest> normalizeNameItems(List<ConfigNameItemRequest> items, String label) {
        List<ConfigNameItemRequest> normalized = items.stream()
                .peek(item -> item.setName(normalizeRequired(item.getName(), label + "名称不能为空")))
                .sorted(Comparator.comparing(item -> resolveSort(item.getSortOrder(), 0)))
                .toList();
        if (normalized.isEmpty()) {
            throw new BaseException("请至少配置 1 个" + label);
        }
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
            List<String> options = normalizeOptions(item.getOptions());
            if (OPTION_ENTRY_FIELD_TYPES.contains(item.getFieldType())) {
                if (options.size() < 2) {
                    throw new BaseException(item.getFieldLabel() + "至少需要 2 个候选项");
                }
                assertUniqueKeys(options, item.getFieldLabel() + "选项");
                item.setOptions(options);
            } else {
                item.setOptions(List.of());
            }
            normalized.add(item);
        }
        normalized = normalized.stream()
                .sorted(Comparator.comparing(item -> resolveSort(item.getSortOrder(), 0)))
                .toList();
        assertUniqueKeys(normalized.stream().map(EntryFieldItemRequest::getFieldKey).toList(), "报名字段 key");
        assertUniqueKeys(normalized.stream().map(EntryFieldItemRequest::getFieldLabel).toList(), "报名字段名称");
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

    private int countDoneChecks(List<CompetitionCheckVO> checks) {
        return (int) checks.stream()
                .filter(check -> CHECK_DONE.equals(check.getState()))
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

    private String writeDimensions(List<DimensionRequest> dimensions) {
        try {
            return objectMapper.writeValueAsString(dimensions);
        } catch (JsonProcessingException ex) {
            throw new BaseException("保存评分维度失败");
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
