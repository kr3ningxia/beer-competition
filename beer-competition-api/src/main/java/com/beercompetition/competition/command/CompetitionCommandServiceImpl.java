package com.beercompetition.competition.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.pojo.dto.CompetitionBaseInfoUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.CompetitionRefundPolicyUpdateRequest;
import com.beercompetition.pojo.enums.CompetitionDeliveryMethod;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.LogisticsVisibility;
import com.beercompetition.pojo.enums.RefundApprovalMode;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.common.context.BaseContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDateTime;
import com.beercompetition.competition.command.CompetitionCommandService;
import com.beercompetition.competition.query.CompetitionQueryService;

/**
 * 执行赛事主记录写操作，事务边界与原有接口保持一致。
 */
@Service
@RequiredArgsConstructor
public class CompetitionCommandServiceImpl implements CompetitionCommandService {

    private static final String DEFAULT_DELIVERY_METHOD = "BOTH";

    private static final String DEFAULT_LOGISTICS_VISIBILITY = "PAYMENT_CONFIRMED";

    private static final String LOG_TARGET_COMPETITION = "COMPETITION";

    private final CompetitionMapper competitionMapper;

    private final AdminOperationLogMapper adminOperationLogMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final ObjectMapper objectMapper;

    private final CompetitionQueryService competitionQueryService;

    private final CompetitionCreationService competitionCreationService;

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
        return competitionQueryService.getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateRefundPolicy(Long id, CompetitionRefundPolicyUpdateRequest request) {
        Competition competition = getCompetitionOrThrow(id);
        LocalDateTime now = LocalDateTime.now();
        if (!isRefundPolicyEditable(competition, now)) {
            throw new BaseException("退款申请时间已截止，审批方式不能修改");
        }
        String oldMode = resolveRefundApprovalMode(competition).name();
        String newMode = RefundApprovalMode.of(request.getRefundApprovalMode()).name();
        if (Objects.equals(oldMode, newMode)) {
            return competitionQueryService.getCompetitionDetail(id);
        }
        competition.setRefundApprovalMode(newMode);
        competitionMapper.updateById(competition);
        writeRefundPolicyLog(competition.getId(), oldMode, newMode);
        return competitionQueryService.getCompetitionDetail(id);
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

    private boolean isRefundPolicyEditable(Competition competition, LocalDateTime now) {
        if (competition == null || parseStatus(competition) == CompetitionStatus.ARCHIVED
                || competition.getRegistrationDeadline() == null) {
            return false;
        }
        return !now.isAfter(competition.getRegistrationDeadline());
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

    private void writeRefundPolicyLog(Long competitionId, String oldMode, String newMode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "修改退款审批方式");
        payload.put("oldMode", oldMode);
        payload.put("newMode", newMode);
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(BaseContext.getCurrentId())
                .action("COMPETITION_REFUND_POLICY_UPDATE")
                .targetType(LOG_TARGET_COMPETITION)
                .targetPublicId(String.valueOf(competitionId))
                .summary(writeObjectJson(payload, "保存退款规则操作日志失败"))
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

    private CompetitionStatus parseStatus(Competition competition) {
        try {
            return CompetitionStatus.valueOf(competition.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BaseException("比赛状态不合法：" + competition.getStatus());
        }
    }

    private boolean hasEntries(Long competitionId) {
        return beerEntryMapper.selectCount(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)) > 0;
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

    @Override
    public CompetitionVO createCompetition(CompetitionCreateRequest request) {
        return competitionCreationService.createCompetition(request);
    }
}
