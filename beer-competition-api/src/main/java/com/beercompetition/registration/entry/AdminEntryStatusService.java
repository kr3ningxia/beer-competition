package com.beercompetition.registration.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.judging.scoring.EntryEvaluationDataService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 维护后台可执行的入库和取消状态转换。
 */
@Service
@RequiredArgsConstructor
public class AdminEntryStatusService {

    private static final Set<String> ACTIVE_REFUND_STATUSES = Set.of(
                EntryRefundStatus.REQUESTED.name(),
                EntryRefundStatus.APPROVED.name(),
                EntryRefundStatus.PROCESSING.name()
        );

    private static final String TARGET_ENTRY = "BEER_ENTRY";

    private final AdminOperationLogMapper adminOperationLogMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionMapper competitionMapper;

    private final EntryPaymentMapper entryPaymentMapper;

    private final EntryRefundMapper entryRefundMapper;

    private final EntryDeliveryMapper entryDeliveryMapper;

    private final EntryEvaluationDataService entryEvaluationDataService;

    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void markStored(Long entryId) {
        markStored(entryId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markStored(Long entryId, AdminEntryStatusRequest request) {
        // 1) 查询作品并校验状态
        BeerEntry entry = requireEntry(entryId);
        if (!EntryStatus.REGISTERED.name().equals(entry.getStatus())) {
            throw new BaseException("只有报名成功的酒款可以确认入库");
        }
        if (hasActiveRefund(entry.getId())) {
            throw new BaseException("退款处理中，不能确认入库");
        }
        EntryDelivery delivery = ensureEntryDelivery(entry.getId());

        // 2) 更新送样记录和入库状态
        delivery.setDeliveryStatus(EntryDeliveryStatus.RECEIVED.name());
        delivery.setReceivedTime(LocalDateTime.now());
        delivery.setReceivedByAdminId(BaseContext.getCurrentId());
        delivery.setReceiveRemark(normalizeStatusReason(request));
        if (delivery.getSubmittedTime() == null) {
            delivery.setSubmittedTime(LocalDateTime.now());
        }
        entryDeliveryMapper.updateById(delivery);
        entry.setStoredFlag(1);
        entry.setStatus(EntryStatus.STORED.name());
        beerEntryMapper.updateById(entry);
        writeEntryLog("ENTRY_MARK_STORED", entry.getUuid(), buildStatusLogSummary("确认入库", normalizeStatusReason(request)));
    }

    @Transactional(rollbackFor = Exception.class)
    public void unmarkStored(Long entryId, AdminEntryStatusRequest request) {
        // 1) 查询作品并校验撤销条件
        BeerEntry entry = requireEntry(entryId);
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        EntryRefund refund = findLatestRefund(entry.getId());
        boolean hasEvaluationData = entryEvaluationDataService.hasEntryEvaluationData(entry.getId());
        if (!canUnmarkStored(entry, competition, refund, hasEvaluationData)) {
            throw new BaseException(resolveUnmarkStoredUnavailableReason(
                    entry, competition, refund, hasEvaluationData));
        }
        String reason = normalizeRequired(request == null ? null : request.getReason(), "请填写撤销入库原因");
        EntryDelivery delivery = ensureEntryDelivery(entry.getId());

        // 2) 回退入库状态并保留寄样信息
        delivery.setDeliveryStatus(EntryDeliveryStatus.SUBMITTED.name());
        delivery.setReceivedTime(null);
        delivery.setReceivedByAdminId(null);
        delivery.setReceiveRemark(reason);
        entryDeliveryMapper.updateById(delivery);

        entry.setStoredFlag(0);
        entry.setStatus(EntryStatus.REGISTERED.name());
        beerEntryMapper.updateById(entry);
        writeEntryLog("ENTRY_UNMARK_STORED", entry.getUuid(), buildStatusLogSummary("撤销入库", reason));
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelEntry(Long entryId) {
        cancelEntry(entryId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelEntry(Long entryId, AdminEntryStatusRequest request) {
        // 1) 查询作品并校验状态
        BeerEntry entry = requireEntry(entryId);
        if (!Set.of(EntryStatus.PENDING_PAYMENT.name(), EntryStatus.REGISTERED.name()).contains(entry.getStatus())) {
            throw new BaseException("当前状态不能取消报名");
        }
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());
        if (EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            throw new BaseException("已支付报名请通过退款申请处理");
        }
        if (EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus())) {
            throw new BaseException("银行转账确认中，请先处理转账记录");
        }

        // 2) 更新支付记录并标记取消
        payment.setStatus(EntryPaymentStatus.CANCELED.name());
        payment.setConfirmRemark(normalizeStatusReason(request));
        entryPaymentMapper.updateById(payment);
        entry.setStatus(EntryStatus.CANCELED.name());
        beerEntryMapper.updateById(entry);
        writeEntryLog("ENTRY_CANCEL", entry.getUuid(), buildStatusLogSummary("取消报名", normalizeStatusReason(request)));
    }

    private String buildStatusLogSummary(String action, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("reason", StringUtils.hasText(reason) ? reason : "");
        return writeObjectJson(payload, "保存状态记录失败");
    }

    private String normalizeStatusReason(AdminEntryStatusRequest request) {
        return request == null ? null : normalizeNullable(request.getReason());
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

    private boolean hasActiveRefund(Long beerEntryId) {
        return isActiveRefund(findLatestRefund(beerEntryId));
    }

    private boolean isActiveRefund(EntryRefund refund) {
        return refund != null && ACTIVE_REFUND_STATUSES.contains(refund.getStatus());
    }

    private boolean canUnmarkStored(BeerEntry entry,
                                    Competition competition,
                                    EntryRefund refund,
                                    boolean hasEvaluationData) {
        if (entry == null || !EntryStatus.STORED.name().equals(entry.getStatus()) || !Objects.equals(entry.getStoredFlag(), 1)) {
            return false;
        }
        if (isActiveRefund(refund) || isResultPublished(competition, entry)) {
            return false;
        }
        return !hasEvaluationData;
    }

    private String resolveUnmarkStoredUnavailableReason(BeerEntry entry,
                                                        Competition competition,
                                                        EntryRefund refund,
                                                        boolean hasEvaluationData) {
        if (entry == null || !EntryStatus.STORED.name().equals(entry.getStatus()) || !Objects.equals(entry.getStoredFlag(), 1)) {
            return "只有已入库酒款可以撤销入库";
        }
        if (isActiveRefund(refund)) {
            return "退款处理中，不能撤销入库";
        }
        if (isResultPublished(competition, entry)) {
            return "结果已发布，不能撤销入库";
        }
        if (hasEvaluationData) {
            return "酒款已进入评审或结果流程，不能撤销入库";
        }
        return "当前酒款不能撤销入库";
    }

    private EntryDelivery findEntryDelivery(Long beerEntryId) {
        return entryDeliveryMapper.selectOne(new LambdaQueryWrapper<EntryDelivery>()
                .eq(EntryDelivery::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
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

    private EntryDelivery ensureEntryDelivery(Long beerEntryId) {
        EntryDelivery delivery = findEntryDelivery(beerEntryId);
        if (delivery != null) {
            return delivery;
        }
        EntryDelivery created = EntryDelivery.builder()
                .beerEntryId(beerEntryId)
                .deliveryStatus(EntryDeliveryStatus.NOT_SUBMITTED.name())
                .build();
        entryDeliveryMapper.insert(created);
        return created;
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

    private String writeObjectJson(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BaseException(errorMessage);
        }
    }
}
