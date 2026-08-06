package com.beercompetition.registration.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.result.PageResult;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.AdminEntryDeleteRequest;
import com.beercompetition.pojo.dto.AdminEntryUpdateRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.vo.AdminEntryDetailVO;
import com.beercompetition.pojo.vo.AdminEntryDeleteImpactVO;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.beercompetition.registration.entry.AdminEntryService;

/**
 * 实现后台报名查询、资料维护和状态操作。
 */
@Service
@RequiredArgsConstructor
public class AdminEntryServiceImpl implements AdminEntryService {

    private static final Set<String> OPTION_FIELD_TYPES = Set.of("select", "multi_select");

    private static final String TARGET_ENTRY = "BEER_ENTRY";

    private static final int EXTRA_FIELD_VALUE_MAX_LENGTH = 255;

    private final AdminOperationLogMapper adminOperationLogMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;

    private final EntryFieldConfigMapper entryFieldConfigMapper;

    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;

    private final ObjectMapper objectMapper;

    private final AdminEntryDetailAssembler adminEntryDetailAssembler;

    private final AdminEntryDeletionService adminEntryDeletionService;

    private final AdminEntryStatusService adminEntryStatusService;

    @Override
    public PageResult<AdminEntryVO> listAdminEntries(Long competitionId, String status, String paymentStatus,
                                                     String deliveryStatus, Long categoryId, Boolean assigned,
                                                     String refundStatus, String keyword, Integer page, Integer pageSize) {
        // 1) 参数规范化与基础查询
        int currentPage = Math.max(page == null ? 1 : page, 1);
        int currentPageSize = Math.min(Math.max(pageSize == null ? 30 : pageSize, 1), 100);
        String normalizedKeyword = normalizeNullable(keyword);
        int offset = (currentPage - 1) * currentPageSize;

        // 2) 由数据库完成关联、筛选、排序和分页，避免为当前页以外的酒款反复查询详情。
        long total = beerEntryMapper.countAdminEntries(competitionId, status, paymentStatus, deliveryStatus,
                categoryId, assigned, refundStatus, normalizedKeyword);
        List<AdminEntryVO> records = total == 0
                ? List.of()
                : beerEntryMapper.selectAdminEntryPage(competitionId, status, paymentStatus, deliveryStatus,
                categoryId, assigned, refundStatus, normalizedKeyword, offset, currentPageSize);
        return new PageResult<>(total, records);
    }

    @Override
    public AdminEntryDetailVO getAdminEntry(Long entryId) {
        // 1) 查询酒款主体
        BeerEntry entry = requireEntry(entryId);

        // 2) 组装后台详情
        return adminEntryDetailAssembler.toAdminEntryDetailVO(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminEntryDetailVO updateAdminEntry(Long entryId, AdminEntryUpdateRequest request) {
        // 1) 查询上下文与前置校验
        BeerEntry entry = requireEntry(entryId);
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        if (EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus()) || isResultPublished(competition, entry)) {
            throw new BaseException("结果已发布，不能直接修改报名信息");
        }
        CompetitionCategory category = requireCompetitionCategory(entry.getCompetitionId(), request.getCategoryId());
        CompetitionStyleConfig selectedStyle = resolveEditableCompetitionStyle(entry, request.getStyle());
        List<EntryFieldConfig> fieldConfigs = listEntryFieldConfigs(entry.getCompetitionId());
        Map<String, String> normalizedExtraFields = normalizeExtraFields(fieldConfigs, request.getExtraFields());
        String reason = normalizeRequired(request.getReason(), "请填写修改原因");

        // 2) 收集变更并更新主记录
        List<Map<String, String>> changes = new ArrayList<>();
        addChange(changes, "酒名", entry.getName(), normalizeRequired(request.getName(), "酒款名称不能为空"));
        addChange(changes, "投递组别", resolveCategoryName(entry.getCategoryId()), category.getName());
        addChange(changes, "基础风格", entry.getStyle(), normalizeRequired(request.getStyle(), "基础风格不能为空"));
        addChange(changes, "ABV", entry.getAbv() == null ? null : entry.getAbv().stripTrailingZeros().toPlainString(),
                request.getAbv() == null ? null : request.getAbv().stripTrailingZeros().toPlainString());
        addExtraFieldChanges(changes, entry.getId(), fieldConfigs, normalizedExtraFields);

        entry.setName(normalizeRequired(request.getName(), "酒款名称不能为空"));
        entry.setCategoryId(category.getId());
        entry.setStyle(normalizeRequired(request.getStyle(), "基础风格不能为空"));
        entry.setStyleConfigId(selectedStyle == null ? entry.getStyleConfigId() : selectedStyle.getId());
        entry.setAbv(request.getAbv());
        entry.setExtraFieldsJson(writeJson(normalizedExtraFields));
        beerEntryMapper.updateById(entry);

        // 3) 重写补充字段并记录审计
        rewriteEntryExtraFields(entry.getId(), fieldConfigs, normalizedExtraFields);
        writeEntryLog("ENTRY_UPDATE", entry.getUuid(), buildEntryLogSummary(reason, changes));

        // 4) 返回最新详情
        return adminEntryDetailAssembler.toAdminEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        CompetitionCategory category = competitionCategoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private void addChange(List<Map<String, String>> changes, String field, String before, String after) {
        String normalizedBefore = normalizeNullable(before);
        String normalizedAfter = normalizeNullable(after);
        if (Objects.equals(normalizedBefore, normalizedAfter)) {
            return;
        }
        Map<String, String> change = new LinkedHashMap<>();
        change.put("field", field);
        change.put("before", normalizedBefore == null ? "" : normalizedBefore);
        change.put("after", normalizedAfter == null ? "" : normalizedAfter);
        changes.add(change);
    }

    private void addExtraFieldChanges(List<Map<String, String>> changes, Long beerEntryId,
                                      List<EntryFieldConfig> fieldConfigs, Map<String, String> normalizedExtraFields) {
        Map<String, String> current = beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .eq(BeerEntryExtraField::getBeerEntryId, beerEntryId))
                .stream()
                .collect(Collectors.toMap(BeerEntryExtraField::getFieldKey, BeerEntryExtraField::getFieldValue, (left, right) -> right));
        for (EntryFieldConfig config : fieldConfigs) {
            addChange(changes, config.getFieldLabel(), current.get(config.getFieldKey()), normalizedExtraFields.get(config.getFieldKey()));
        }
    }

    private void rewriteEntryExtraFields(Long beerEntryId, List<EntryFieldConfig> fieldConfigs,
                                         Map<String, String> normalizedExtraFields) {
        List<String> activeKeys = fieldConfigs.stream().map(EntryFieldConfig::getFieldKey).toList();
        if (!activeKeys.isEmpty()) {
            beerEntryExtraFieldMapper.delete(new LambdaQueryWrapper<BeerEntryExtraField>()
                    .eq(BeerEntryExtraField::getBeerEntryId, beerEntryId)
                    .in(BeerEntryExtraField::getFieldKey, activeKeys));
        }
        for (EntryFieldConfig fieldConfig : fieldConfigs) {
            String value = normalizedExtraFields.get(fieldConfig.getFieldKey());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            beerEntryExtraFieldMapper.insert(BeerEntryExtraField.builder()
                    .beerEntryId(beerEntryId)
                    .fieldKey(fieldConfig.getFieldKey())
                    .fieldLabel(fieldConfig.getFieldLabel())
                    .fieldValue(value)
                    .build());
        }
    }

    private String buildEntryLogSummary(String reason, List<Map<String, String>> changes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", reason);
        payload.put("changes", changes);
        return writeObjectJson(payload, "保存修改记录失败");
    }

    private void writeEntryLog(String action, String targetPublicId, String summary) {
        Long adminId = BaseContext.getCurrentId();
        if (adminId == null) {
            return;
        }
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(adminId)
                .action(action)
                .targetType(TARGET_ENTRY)
                .targetPublicId(targetPublicId)
                .summary(summary)
                .build());
    }

    private Map<String, String> normalizeExtraFields(List<EntryFieldConfig> configs, Map<String, Object> input) {
        Map<String, Object> source = input == null ? Map.of() : input;
        Map<String, String> normalized = new LinkedHashMap<>();
        for (EntryFieldConfig config : configs) {
            Object rawValue = source.get(config.getFieldKey());
            String value = normalizeFieldValue(config, rawValue);
            if (Objects.equals(config.getRequiredFlag(), 1) && !StringUtils.hasText(value)) {
                throw new BaseException("请填写" + config.getFieldLabel());
            }
            if (value != null && value.length() > EXTRA_FIELD_VALUE_MAX_LENGTH) {
                throw new BaseException(config.getFieldLabel() + "不能超过" + EXTRA_FIELD_VALUE_MAX_LENGTH + "个字符");
            }
            if (StringUtils.hasText(value)) {
                normalized.put(config.getFieldKey(), value);
            }
        }
        return normalized;
    }

    private String normalizeFieldValue(EntryFieldConfig config, Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if ("multi_select".equals(config.getFieldType())) {
            List<String> values = normalizeMultiSelectValue(config, rawValue);
            return values.isEmpty() ? null : String.join("、", values);
        }
        if ("number".equals(config.getFieldType())) {
            BigDecimal number = asBigDecimal(rawValue);
            return number == null ? null : number.stripTrailingZeros().toPlainString();
        }
        String value = normalizeNullable(String.valueOf(rawValue));
        if (OPTION_FIELD_TYPES.contains(config.getFieldType()) && StringUtils.hasText(value)) {
            requireAllowedOption(config, value);
        }
        return value;
    }

    private List<String> normalizeMultiSelectValue(EntryFieldConfig config, Object rawValue) {
        List<?> rawItems;
        if (rawValue instanceof List<?> list) {
            rawItems = list;
        } else {
            rawItems = List.of(rawValue);
        }
        List<String> values = new ArrayList<>();
        for (Object item : rawItems) {
            String value = normalizeNullable(String.valueOf(item));
            if (!StringUtils.hasText(value)) {
                continue;
            }
            requireAllowedOption(config, value);
            values.add(value);
        }
        return values;
    }

    private void requireAllowedOption(EntryFieldConfig config, String value) {
        List<String> options = readOptions(config.getOptionsJson());
        if (!options.isEmpty() && !options.contains(value)) {
            throw new BaseException(config.getFieldLabel() + "选项不合法");
        }
    }

    private CompetitionCategory requireCompetitionCategory(Long competitionId, Long categoryId) {
        CompetitionCategory category = competitionCategoryMapper.selectOne(new LambdaQueryWrapper<CompetitionCategory>()
                .eq(CompetitionCategory::getId, categoryId)
                .eq(CompetitionCategory::getCompetitionId, competitionId));
        if (category == null) {
            throw new BaseException("投递组别不属于当前赛事");
        }
        return category;
    }

    private CompetitionStyleConfig requireCompetitionStyle(Long competitionId, String styleName) {
        CompetitionStyleConfig style = competitionStyleConfigMapper.selectOne(new LambdaQueryWrapper<CompetitionStyleConfig>()
                .eq(CompetitionStyleConfig::getCompetitionId, competitionId)
                .eq(CompetitionStyleConfig::getName, normalizeRequired(styleName, "基础风格不能为空"))
                .eq(CompetitionStyleConfig::getActiveFlag, 1)
                .orderByDesc(CompetitionStyleConfig::getId)
                .last("LIMIT 1"));
        if (style == null) {
            throw new BaseException("基础风格不属于当前赛事");
        }
        return style;
    }

    private CompetitionStyleConfig resolveEditableCompetitionStyle(BeerEntry entry, String styleName) {
        String normalized = normalizeRequired(styleName, "基础风格不能为空");
        return Objects.equals(entry.getStyle(), normalized) ? null : requireCompetitionStyle(entry.getCompetitionId(), normalized);
    }

    private List<EntryFieldConfig> listEntryFieldConfigs(Long competitionId) {
        return entryFieldConfigMapper.selectList(new LambdaQueryWrapper<EntryFieldConfig>()
                .eq(EntryFieldConfig::getCompetitionId, competitionId)
                .eq(EntryFieldConfig::getActiveFlag, 1)
                .orderByAsc(EntryFieldConfig::getSortOrder)
                .orderByAsc(EntryFieldConfig::getId));
    }

    private BeerEntry requireEntry(Long entryId) {
        BeerEntry entry = beerEntryMapper.selectById(entryId);
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        return entry;
    }

    private boolean isResultPublished(Competition competition, BeerEntry entry) {
        return EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus())
                || (competition != null && CompetitionStatus.PUBLISHED.name().equals(competition.getStatus()));
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

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new BaseException("数字格式不正确");
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
            throw new BaseException("解析字段选项失败");
        }
    }

    private String writeJson(Map<String, String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BaseException("保存补充字段失败");
        }
    }

    private String writeObjectJson(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BaseException(errorMessage);
        }
    }

    @Override
    public AdminEntryDeleteImpactVO getAdminEntryDeleteImpact(Long entryId) {
        return adminEntryDeletionService.getAdminEntryDeleteImpact(entryId);
    }

    @Override
    public void administrativelyDeleteEntry(Long entryId, AdminEntryDeleteRequest request) {
        adminEntryDeletionService.administrativelyDeleteEntry(entryId, request);
    }

    @Override
    public void markStored(Long entryId) {
        adminEntryStatusService.markStored(entryId);
    }

    @Override
    public void markStored(Long entryId, AdminEntryStatusRequest request) {
        adminEntryStatusService.markStored(entryId, request);
    }

    @Override
    public void unmarkStored(Long entryId, AdminEntryStatusRequest request) {
        adminEntryStatusService.unmarkStored(entryId, request);
    }

    @Override
    public void cancelEntry(Long entryId) {
        adminEntryStatusService.cancelEntry(entryId);
    }

    @Override
    public void cancelEntry(Long entryId, AdminEntryStatusRequest request) {
        adminEntryStatusService.cancelEntry(entryId, request);
    }
}
