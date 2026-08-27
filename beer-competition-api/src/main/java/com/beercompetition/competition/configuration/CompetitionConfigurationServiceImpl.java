package com.beercompetition.competition.configuration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
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
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.pojo.vo.StyleItemVO;
import com.beercompetition.service.StyleLibraryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.beercompetition.competition.configuration.CompetitionConfigurationService;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.competition.query.CompetitionQueryService;

/**
 * 保存赛事配置快照，并保护已经产生业务数据的配置项。
 */
@Service
@RequiredArgsConstructor
public class CompetitionConfigurationServiceImpl implements CompetitionConfigurationService {

    private static final BigDecimal SCORE_FORM_TOTAL = BigDecimal.valueOf(50);

    private static final int FLAG_FALSE = 0;

    private static final int FLAG_TRUE = 1;

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

    private static final Set<String> ENTRY_FIELD_TYPES = Set.of("text", "textarea", "number", "select", "multi_select");

    private static final Set<String> OPTION_ENTRY_FIELD_TYPES = Set.of("select", "multi_select");

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;

    private final EntryFieldConfigMapper entryFieldConfigMapper;

    private final JudgeTableMapper judgeTableMapper;

    private final JudgeAssignmentMapper judgeAssignmentMapper;

    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final StyleLibraryService styleLibraryService;

    private final ObjectMapper objectMapper;

    private final CompetitionQueryService competitionQueryService;

    private final CompetitionAccessService competitionAccessService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateCategories(Long id, ConfigNameBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertCategoriesEditable(competition);
        List<ConfigNameItemRequest> items = normalizeNameItems(request.getItems(), "投递组别");

        // 2) 替换当前比赛投递组别
        replaceCategories(id, items);

        // 3) 重新计算配置检查并返回详情
        return competitionQueryService.getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateStyles(Long id, CompetitionStyleLibraryUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertRegistrationConfigEditable(competition, "基础风格库");
        String styleLibraryVersion = normalizeRequired(request.getStyleLibraryVersion(), "基础风格库不能为空");
        List<StyleItemVO> snapshotStyles = styleLibraryService.listEnabledStyles(styleLibraryVersion);
        competition.setStyleLibraryVersion(styleLibraryVersion);

        // 2) 更新比赛风格库版本并重建比赛风格快照
        competitionMapper.updateById(competition);
        replaceStyleSnapshot(id, styleLibraryVersion, snapshotStyles);

        // 3) 重新计算配置检查并返回详情
        return competitionQueryService.getCompetitionDetail(id);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateEntryFields(Long id, EntryFieldBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertRegistrationConfigEditable(competition, "报名补充字段");
        List<EntryFieldItemRequest> items = normalizeEntryFields(request.getItems());

        // 2) 差异更新当前字段，停用项及历史填写值继续保留
        reconcileEntryFields(id, items);

        // 3) 重新计算配置检查并返回详情
        return competitionQueryService.getCompetitionDetail(id);
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
        return competitionQueryService.getCompetitionDetail(id);
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

    private void reconcileEntryFields(Long competitionId, List<EntryFieldItemRequest> items) {
        List<EntryFieldConfig> existing = entryFieldConfigMapper.selectList(new LambdaQueryWrapper<EntryFieldConfig>()
                .eq(EntryFieldConfig::getCompetitionId, competitionId));
        Map<String, EntryFieldConfig> existingByKey = existing.stream()
                .collect(Collectors.toMap(EntryFieldConfig::getFieldKey, Function.identity(), (left, right) -> left));
        assertEntryFieldChangesCompatible(competitionId, items, existingByKey);

        Set<String> nextKeys = items.stream().map(EntryFieldItemRequest::getFieldKey).collect(Collectors.toSet());
        for (EntryFieldConfig config : existing) {
            if (!nextKeys.contains(config.getFieldKey()) && Objects.equals(config.getActiveFlag(), FLAG_TRUE)) {
                config.setActiveFlag(FLAG_FALSE);
                entryFieldConfigMapper.updateById(config);
            }
        }

        int sort = 0;
        for (EntryFieldItemRequest item : items) {
            EntryFieldConfig config = existingByKey.get(item.getFieldKey());
            if (config == null) {
                config = EntryFieldConfig.builder()
                        .competitionId(competitionId)
                        .fieldKey(item.getFieldKey())
                        .build();
            }
            config.setFieldLabel(item.getFieldLabel());
            config.setFieldType(item.getFieldType());
            config.setHelpText(normalizeNullable(item.getHelpText()));
            config.setOptionsJson(writeOptions(item.getOptions()));
            config.setRequiredFlag(Boolean.TRUE.equals(item.getRequired()) ? FLAG_TRUE : FLAG_FALSE);
            config.setVisibleToJudges(Boolean.TRUE.equals(item.getVisibleToJudges()) ? FLAG_TRUE : FLAG_FALSE);
            config.setSortOrder(resolveSort(item.getSortOrder(), sort++));
            config.setActiveFlag(FLAG_TRUE);
            if (config.getId() == null) {
                entryFieldConfigMapper.insert(config);
            } else {
                entryFieldConfigMapper.updateById(config);
            }
        }
    }

    private void assertEntryFieldChangesCompatible(Long competitionId,
                                                   List<EntryFieldItemRequest> items,
                                                   Map<String, EntryFieldConfig> existingByKey) {
        List<Long> activeEntryIds = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                        .eq(BeerEntry::getCompetitionId, competitionId)
                        .eq(BeerEntry::getDeletedFlag, FLAG_FALSE)
                        .ne(BeerEntry::getStatus, EntryStatus.CANCELED.name()))
                .stream().map(BeerEntry::getId).toList();
        if (activeEntryIds.isEmpty()) {
            return;
        }
        Map<String, List<String>> valuesByKey = beerEntryExtraFieldMapper.selectList(
                        new LambdaQueryWrapper<BeerEntryExtraField>().in(BeerEntryExtraField::getBeerEntryId, activeEntryIds))
                .stream()
                .collect(Collectors.groupingBy(BeerEntryExtraField::getFieldKey,
                        Collectors.mapping(BeerEntryExtraField::getFieldValue, Collectors.toList())));
        for (EntryFieldItemRequest item : items) {
            List<String> values = valuesByKey.getOrDefault(item.getFieldKey(), List.of()).stream()
                    .filter(StringUtils::hasText).toList();
            if (Boolean.TRUE.equals(item.getRequired()) && values.size() < activeEntryIds.size()) {
                throw new BaseException(item.getFieldLabel() + "存在未填写的报名酒款，暂不能设为必填");
            }
            if (values.isEmpty()) {
                continue;
            }
            if ("select".equals(item.getFieldType()) && values.stream().anyMatch(value -> !item.getOptions().contains(value))) {
                throw new BaseException(item.getFieldLabel() + "已有填写值不在新选项中");
            }
            if ("multi_select".equals(item.getFieldType()) && values.stream()
                    .flatMap(value -> Arrays.stream(value.split("、")))
                    .anyMatch(value -> !item.getOptions().contains(value))) {
                throw new BaseException(item.getFieldLabel() + "已有多选值不在新选项中");
            }
            EntryFieldConfig previous = existingByKey.get(item.getFieldKey());
            if (previous != null && !Objects.equals(previous.getFieldType(), item.getFieldType())
                    && !(Set.of("text", "textarea").contains(previous.getFieldType())
                    && Set.of("text", "textarea").contains(item.getFieldType()))) {
                throw new BaseException(item.getFieldLabel() + "已有填写数据，暂不能修改字段类型");
            }
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

    private List<ScoreConfigVO> listScoreConfigs(Long competitionId) {
        return competitionScoreConfigMapper.selectList(new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionScoreConfig::getId))
                .stream()
                .map(this::toScoreConfigVO)
                .toList();
    }

    private void assertCategoriesEditable(Competition competition) {
        if (parseStatus(competition) != CompetitionStatus.DRAFT) {
            throw new BaseException("报名已开放，投递组别已锁定");
        }
    }

    private void assertRegistrationConfigEditable(Competition competition, String label) {
        CompetitionStatus status = parseStatus(competition);
        if (status == CompetitionStatus.DRAFT || status == CompetitionStatus.REGISTRATION_OPEN) {
            return;
        }
        throw new BaseException("当前阶段不能修改" + label);
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

    private Competition getCompetitionOrThrow(Long id) {
        competitionAccessService.requireCompetitionAccess(id);
        Competition competition = competitionMapper.selectById(id);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private CompetitionStatus parseStatus(Competition competition) {
        try {
            return CompetitionStatus.valueOf(competition.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BaseException("比赛状态不合法：" + competition.getStatus());
        }
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
