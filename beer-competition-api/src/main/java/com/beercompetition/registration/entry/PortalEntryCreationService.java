package com.beercompetition.registration.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.service.EntryScanLabelService;
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
import java.util.UUID;

/**
 * 创建厂商报名及其初始付款、送样和扩展字段记录。
 */
@Service
@RequiredArgsConstructor
public class PortalEntryCreationService {

    private static final Set<String> OPTION_FIELD_TYPES = Set.of("select", "multi_select");

    private static final int EXTRA_FIELD_VALUE_MAX_LENGTH = 255;

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;

    private final EntryPaymentMapper entryPaymentMapper;

    private final EntryDeliveryMapper entryDeliveryMapper;

    private final EntryFieldConfigMapper entryFieldConfigMapper;

    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final ObjectMapper objectMapper;

    private final PortalEntryViewAssembler portalEntryViewAssembler;

    private final PortalAccountAccessService portalAccountAccessService;

    @Transactional(rollbackFor = Exception.class)
    public EntryDetailVO submitPortalEntry(Long competitionId, PortalEntrySubmitRequest request) {
        // 1) 参数规范化与前置校验
        PortalAccount account = portalAccountAccessService.requireCurrentAccount();
        Competition competition = requireOpenCompetition(competitionId);
        requireRulesAcceptedIfConfigured(competition, request);
        CompetitionCategory category = requireCompetitionCategory(competitionId, request.getCategoryId());
        CompetitionStyleConfig selectedStyle = requireCompetitionStyle(competitionId, request.getStyle());
        List<EntryFieldConfig> fieldConfigs = listEntryFieldConfigs(competitionId);
        Map<String, String> normalizedExtraFields = normalizeExtraFields(fieldConfigs, request.getExtraFields());

        // 2) 创建作品主记录
        BeerEntry entry = BeerEntry.builder()
                .uuid(generateEntryUuid())
                .competitionId(competition.getId())
                .breweryId(account.getBreweryId())
                .categoryId(category.getId())
                .name(normalizeRequired(request.getName(), "酒款名称不能为空"))
                .style(normalizeRequired(request.getStyle(), "基础风格不能为空"))
                .styleConfigId(selectedStyle.getId())
                .abv(request.getAbv())
                .extraFieldsJson(writeJson(normalizedExtraFields))
                .status(EntryStatus.PENDING_PAYMENT.name())
                .storedFlag(0)
                .build();
        beerEntryMapper.insert(entry);
        entryScanLabelService.createActiveLabel(entry, BaseContext.getCurrentId());

        // 3) 初始化支付与送样记录
        entryPaymentMapper.insert(EntryPayment.builder()
                .beerEntryId(entry.getId())
                .amount(resolveEntryFee(competition, LocalDateTime.now()))
                .status(EntryPaymentStatus.UNPAID.name())
                .payMethod(EntryPayMethod.MANUAL.name())
                .build());
        entryDeliveryMapper.insert(EntryDelivery.builder()
                .beerEntryId(entry.getId())
                .deliveryStatus(EntryDeliveryStatus.NOT_SUBMITTED.name())
                .build());

        // 4) 写入赛事配置内的补充字段
        for (EntryFieldConfig fieldConfig : fieldConfigs) {
            String value = normalizedExtraFields.get(fieldConfig.getFieldKey());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            beerEntryExtraFieldMapper.insert(BeerEntryExtraField.builder()
                    .beerEntryId(entry.getId())
                    .fieldKey(fieldConfig.getFieldKey())
                    .fieldLabel(fieldConfig.getFieldLabel())
                    .fieldValue(value)
                    .build());
        }

        // 5) 返回新作品详情
        return portalEntryViewAssembler.toEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
    }

    private void requireRulesAcceptedIfConfigured(Competition competition, PortalEntrySubmitRequest request) {
        if (StringUtils.hasText(competition.getRulesUrl()) && !Boolean.TRUE.equals(request.getRulesAccepted())) {
            throw new BaseException("请先阅读并同意本次大赛参赛细则");
        }
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

    private Competition requireOpenCompetition(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (!CompetitionStatus.REGISTRATION_OPEN.name().equals(competition.getStatus())) {
            throw new BaseException("当前赛事暂未开放报名");
        }
        LocalDateTime now = LocalDateTime.now();
        if (competition.getRegistrationDeadline() != null && now.isAfter(competition.getRegistrationDeadline())) {
            throw new BaseException("报名已截止");
        }
        return competition;
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

    private List<EntryFieldConfig> listEntryFieldConfigs(Long competitionId) {
        return entryFieldConfigMapper.selectList(new LambdaQueryWrapper<EntryFieldConfig>()
                .eq(EntryFieldConfig::getCompetitionId, competitionId)
                .eq(EntryFieldConfig::getActiveFlag, 1)
                .orderByAsc(EntryFieldConfig::getSortOrder)
                .orderByAsc(EntryFieldConfig::getId));
    }

    private String generateEntryUuid() {
        String uuid;
        do {
            uuid = "BE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (beerEntryMapper.selectOne(new LambdaQueryWrapper<BeerEntry>().eq(BeerEntry::getUuid, uuid)) != null);
        return uuid;
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
