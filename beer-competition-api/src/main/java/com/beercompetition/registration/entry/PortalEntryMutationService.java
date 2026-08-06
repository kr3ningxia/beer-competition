package com.beercompetition.registration.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.pojo.dto.PortalEntryUpdateRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 修改或取消厂商报名，并记录可审计的字段变化。
 */
@Service
@RequiredArgsConstructor
public class PortalEntryMutationService {

    private static final Set<String> OPTION_FIELD_TYPES = Set.of("select", "multi_select");

    private static final String TARGET_ENTRY = "BEER_ENTRY";

    private static final int EXTRA_FIELD_VALUE_MAX_LENGTH = 255;

    private final AdminOperationLogMapper adminOperationLogMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionMapper competitionMapper;

    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;

    private final EntryPaymentMapper entryPaymentMapper;

    private final EntryRefundMapper entryRefundMapper;

    private final EntryFieldConfigMapper entryFieldConfigMapper;

    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;

    private final ObjectMapper objectMapper;

    private final PortalEntryViewAssembler portalEntryViewAssembler;

    private final PortalEntryEditPolicy portalEntryEditPolicy;

    private final PortalAccountAccessService portalAccountAccessService;

    @Transactional(rollbackFor = Exception.class)
    public EntryDetailVO updatePortalEntry(Long entryId, PortalEntryUpdateRequest request) {
        // 1) 查询厂商酒款并校验自助修改窗口
        PortalAccount account = portalAccountAccessService.requireCurrentAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        EntryRefund latestRefund = findLatestRefund(entry.getId());
        portalEntryEditPolicy.requireEditable(entry, competition, latestRefund,
                competition != null && isResultPublished(competition, entry));

        // 2) 校验可变字段，投递组别和现场标签不参与修改
        CompetitionStyleConfig selectedStyle = resolveEditableCompetitionStyle(entry, request.getStyle());
        List<EntryFieldConfig> fieldConfigs = listEntryFieldConfigs(entry.getCompetitionId());
        Map<String, String> normalizedExtraFields = normalizeExtraFields(fieldConfigs, request.getExtraFields());
        String nextName = normalizeRequired(request.getName(), "酒款名称不能为空");
        String nextStyle = normalizeRequired(request.getStyle(), "基础风格不能为空");

        List<Map<String, String>> changes = new ArrayList<>();
        addChange(changes, "酒名", entry.getName(), nextName);
        addChange(changes, "基础风格", entry.getStyle(), nextStyle);
        addChange(changes, "ABV", entry.getAbv() == null ? null : entry.getAbv().stripTrailingZeros().toPlainString(),
                request.getAbv() == null ? null : request.getAbv().stripTrailingZeros().toPlainString());
        addExtraFieldChanges(changes, entry.getId(), fieldConfigs, normalizedExtraFields);

        // 3) 更新酒款资料，保留原投递组别、参赛编号和二维码
        entry.setName(nextName);
        entry.setStyle(nextStyle);
        entry.setStyleConfigId(selectedStyle == null ? entry.getStyleConfigId() : selectedStyle.getId());
        entry.setAbv(request.getAbv());
        entry.setExtraFieldsJson(writeJson(normalizedExtraFields));
        beerEntryMapper.updateById(entry);
        rewriteEntryExtraFields(entry.getId(), fieldConfigs, normalizedExtraFields);

        // 4) 写入操作记录并返回最新详情
        if (!changes.isEmpty()) {
            writeEntryLog("ENTRY_PORTAL_UPDATE", entry.getUuid(), buildPortalEntryUpdateLogSummary(changes));
        }
        return portalEntryViewAssembler.toEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public EntryDetailVO cancelPortalEntry(Long entryId) {
        // 1) 查询厂商酒款并校验取消条件
        PortalAccount account = portalAccountAccessService.requireCurrentAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            throw new BaseException("当前酒款不能取消报名");
        }
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());
        assertStandalonePaymentAction(payment, "该酒款已加入统一付款订单，不能单独取消报名");
        if (EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            throw new BaseException("已支付报名请通过退款申请处理");
        }
        if (EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus())) {
            throw new BaseException("银行转账确认中，请先修改或等待转账确认");
        }

        // 2) 取消未付款报名并返回详情
        payment.setStatus(EntryPaymentStatus.CANCELED.name());
        payment.setConfirmRemark("厂商取消报名");
        entryPaymentMapper.updateById(payment);
        entry.setStatus(EntryStatus.CANCELED.name());
        beerEntryMapper.updateById(entry);
        writeEntryLog("ENTRY_PORTAL_CANCEL", entry.getUuid(), "厂商取消报名");
        return portalEntryViewAssembler.toEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
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

    private String buildPortalEntryUpdateLogSummary(List<Map<String, String>> changes) {
        String fields = changes.stream()
                .map(change -> change.get("field"))
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("、"));
        return StringUtils.hasText(fields) ? "厂商自助修改报名资料：" + fields : "厂商自助修改报名资料";
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

    private EntryPayment findEntryPayment(Long beerEntryId) {
        return entryPaymentMapper.selectOne(new LambdaQueryWrapper<EntryPayment>()
                .eq(EntryPayment::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
    }

    private EntryRefund findLatestRefund(Long beerEntryId) {
        return entryRefundMapper.selectOne(new LambdaQueryWrapper<EntryRefund>()
                .eq(EntryRefund::getBeerEntryId, beerEntryId)
                .orderByDesc(EntryRefund::getId)
                .last("LIMIT 1"));
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

    private EntryPayment ensureEntryPayment(Long beerEntryId, Long competitionId) {
        EntryPayment payment = findEntryPayment(beerEntryId);
        if (payment != null) {
            return payment;
        }
        Competition competition = competitionMapper.selectById(competitionId);
        EntryPayment created = EntryPayment.builder()
                .beerEntryId(beerEntryId)
                .amount(resolveEntryFee(competition, LocalDateTime.now()))
                .status(EntryPaymentStatus.UNPAID.name())
                .payMethod(EntryPayMethod.MANUAL.name())
                .build();
        entryPaymentMapper.insert(created);
        return created;
    }

    private void assertStandalonePaymentAction(EntryPayment payment, String message) {
        if (payment != null && payment.getPaymentOrderId() != null) {
            throw new BaseException(message);
        }
    }

    private BigDecimal resolveEntryFee(Competition competition, LocalDateTime now) {
        if (competition == null) {
            return BigDecimal.ZERO;
        }
        if (competition.getEarlyBirdFee() != null
                && competition.getEarlyBirdDeadline() != null
                && !now.isAfter(competition.getEarlyBirdDeadline())) {
            return competition.getEarlyBirdFee();
        }
        return competition.getEntryFee() == null ? BigDecimal.ZERO : competition.getEntryFee();
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

    private BeerEntry requireOwnedEntry(Long entryId, Long breweryId) {
        BeerEntry entry = requireEntry(entryId);
        if (!entry.getBreweryId().equals(breweryId)) {
            throw new ForbiddenException("无权查看该酒款");
        }
        return entry;
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
}
