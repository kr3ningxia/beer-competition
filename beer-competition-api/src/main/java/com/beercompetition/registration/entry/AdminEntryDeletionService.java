package com.beercompetition.registration.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.judging.scoring.EntryEvaluationDataService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BankTransferPaymentMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.PaymentOrderItemMapper;
import com.beercompetition.mapper.PaymentOrderMapper;
import com.beercompetition.mapper.RegistrationBatchMapper;
import com.beercompetition.pojo.dto.AdminEntryDeleteRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BankTransferPayment;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.PaymentOrder;
import com.beercompetition.pojo.po.PaymentOrderItem;
import com.beercompetition.pojo.po.RegistrationBatch;
import com.beercompetition.pojo.vo.AdminEntryDeleteImpactVO;
import com.beercompetition.service.EntryScanLabelService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 评估并执行管理端酒款删除，保持订单、批次和付款状态一致。
 */
@Service
@RequiredArgsConstructor
public class AdminEntryDeletionService {

    private static final String TARGET_ENTRY = "BEER_ENTRY";

    private final AdminOperationLogMapper adminOperationLogMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final BankTransferPaymentMapper bankTransferPaymentMapper;

    private final CompetitionMapper competitionMapper;

    private final EntryPaymentMapper entryPaymentMapper;

    private final PaymentOrderItemMapper paymentOrderItemMapper;

    private final PaymentOrderMapper paymentOrderMapper;

    private final EntryDeliveryMapper entryDeliveryMapper;

    private final RegistrationBatchMapper registrationBatchMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final ObjectMapper objectMapper;

    private final EntryEvaluationDataService entryEvaluationDataService;

    private final CompetitionAccessService competitionAccessService;

    public AdminEntryDeleteImpactVO getAdminEntryDeleteImpact(Long entryId) {
        BeerEntry entry = requireEntry(entryId);
        EntryPayment payment = findEntryPayment(entryId);
        EntryDelivery delivery = findEntryDelivery(entryId);
        EntryScanLabel label = entryScanLabelService.listActiveLabels(List.of(entryId)).get(entryId);
        EntryEvaluationDataService.EntryEvaluationImpact evaluationImpact =
                entryEvaluationDataService.summarizeDeleteImpact(entryId);
        boolean resultPublished = EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus())
                || evaluationImpact.publishedAward();
        boolean highRisk = evaluationImpact.hasEvaluationData() || resultPublished;
        return AdminEntryDeleteImpactVO.builder()
                .entryId(entryId)
                .entryName(entry.getName())
                .shortCode(label == null ? null : label.getShortCode())
                .status(entry.getStatus())
                .paymentStatus(payment == null ? EntryPaymentStatus.UNPAID.name() : payment.getStatus())
                .payMethod(payment == null ? null : payment.getPayMethod())
                .deliveryStatus(delivery == null ? EntryDeliveryStatus.NOT_SUBMITTED.name() : delivery.getDeliveryStatus())
                .roundAssignmentCount(evaluationImpact.assignmentCount())
                .scoreSessionCount(evaluationImpact.scoreSessionCount())
                .scoreRecordCount(evaluationImpact.scoreRecordCount())
                .roundResultCount(evaluationImpact.roundResultCount())
                .awardResultCount(evaluationImpact.awardResultCount())
                .resultPublished(resultPublished)
                .highRisk(highRisk)
                .refundConfirmationRequired(payment != null && EntryPaymentStatus.PAID.name().equals(payment.getStatus()))
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void administrativelyDeleteEntry(Long entryId, AdminEntryDeleteRequest request) {
        BeerEntry entry = requireEntry(entryId);
        AdminEntryDeleteImpactVO impact = getAdminEntryDeleteImpact(entryId);
        String reason = normalizeRequired(request.getReason(), "请填写删除原因");
        if (!StringUtils.hasText(impact.getShortCode())
                || !impact.getShortCode().equalsIgnoreCase(normalizeRequired(request.getConfirmationCode(), "请输入酒款短编号确认"))) {
            throw new BaseException("酒款短编号不匹配");
        }
        if (Boolean.TRUE.equals(impact.getHighRisk()) && !Boolean.TRUE.equals(request.getHighRiskConfirmed())) {
            throw new BaseException("该酒款已有评审或发布数据，请确认高风险删除");
        }
        EntryPayment payment = findEntryPayment(entryId);
        assertAdministrativeDeletePaymentReady(payment, request.getPaymentDisposition());

        entryEvaluationDataService.deleteEntryEvaluationData(entryId);
        entryScanLabelService.disableActiveLabel(entryId);
        cancelPendingPaymentForDeletedEntry(payment, reason);

        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        if (Boolean.TRUE.equals(impact.getResultPublished()) && competition != null
                && Set.of(CompetitionStatus.PUBLISHED.name(), CompetitionStatus.ARCHIVED.name()).contains(competition.getStatus())) {
            competition.setStatus(CompetitionStatus.RESULT_CONFIRMING.name());
            competitionMapper.updateById(competition);
        }

        writeEntryLog("ENTRY_ADMIN_DELETE", entry.getUuid(), buildAdministrativeDeleteLogSummary(reason, impact));
        entry.setDeletedTime(LocalDateTime.now());
        entry.setDeletedByAdminId(BaseContext.getCurrentId());
        entry.setDeleteReason(reason);
        beerEntryMapper.updateById(entry);
        beerEntryMapper.deleteById(entryId);
        refreshRegistrationBatch(entry.getRegistrationBatchId());
    }

    private void assertAdministrativeDeletePaymentReady(EntryPayment payment, String disposition) {
        if (payment == null || Set.of(EntryPaymentStatus.UNPAID.name(), EntryPaymentStatus.CANCELED.name(),
                EntryPaymentStatus.REFUNDED.name()).contains(payment.getStatus())) {
            return;
        }
        if (EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus())) {
            return;
        }
        if (EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            if (!"MANUAL_REFUNDED".equals(disposition)) {
                throw new BaseException("已支付酒款需先完成退款，并确认退款已完成");
            }
            payment.setStatus(EntryPaymentStatus.REFUNDED.name());
            payment.setConfirmRemark("管理端删除酒款，已确认退款");
            entryPaymentMapper.updateById(payment);
            PaymentOrderItem item = paymentOrderItemMapper.selectOne(new LambdaQueryWrapper<PaymentOrderItem>()
                    .eq(PaymentOrderItem::getEntryPaymentId, payment.getId()).last("LIMIT 1"));
            if (item != null) {
                item.setRefundedAmount(item.getAmount());
                item.setStatus(EntryPaymentStatus.REFUNDED.name());
                paymentOrderItemMapper.updateById(item);
                refreshAggregatePaymentStatus(item.getPaymentOrderId());
            }
            return;
        }
        throw new BaseException("当前支付状态不能删除酒款");
    }

    private void cancelPendingPaymentForDeletedEntry(EntryPayment payment, String reason) {
        if (payment == null || !(EntryPaymentStatus.UNPAID.name().equals(payment.getStatus())
                || EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus()))) {
            return;
        }
        if (payment.getBankTransferId() != null) {
            BankTransferPayment transfer = bankTransferPaymentMapper.selectById(payment.getBankTransferId());
            if (transfer != null) {
                transfer.setStatus("CANCELED");
                transfer.setAdminId(BaseContext.getCurrentId());
                transfer.setAdminNote(reason);
                transfer.setProcessedTime(LocalDateTime.now());
                bankTransferPaymentMapper.updateById(transfer);
            }
        }
        payment.setStatus(EntryPaymentStatus.CANCELED.name());
        payment.setConfirmRemark(reason);
        entryPaymentMapper.updateById(payment);
        PaymentOrderItem item = paymentOrderItemMapper.selectOne(new LambdaQueryWrapper<PaymentOrderItem>()
                .eq(PaymentOrderItem::getEntryPaymentId, payment.getId()).last("LIMIT 1"));
        if (item != null) {
            item.setStatus(EntryPaymentStatus.CANCELED.name());
            paymentOrderItemMapper.updateById(item);
            refreshAggregatePaymentStatus(item.getPaymentOrderId());
        }
    }

    private void refreshAggregatePaymentStatus(Long paymentOrderId) {
        if (paymentOrderId == null) {
            return;
        }
        PaymentOrder order = paymentOrderMapper.selectById(paymentOrderId);
        if (order == null) {
            return;
        }
        List<PaymentOrderItem> items = paymentOrderItemMapper.selectList(new LambdaQueryWrapper<PaymentOrderItem>()
                .eq(PaymentOrderItem::getPaymentOrderId, paymentOrderId));
        BigDecimal activeAmount = items.stream()
                .filter(item -> !EntryPaymentStatus.CANCELED.name().equals(item.getStatus()))
                .map(PaymentOrderItem::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundedAmount = items.stream().map(PaymentOrderItem::getRefundedAmount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setAmount(activeAmount);
        order.setRefundedAmount(refundedAmount);
        if (activeAmount.compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus("CANCELED");
        } else if (refundedAmount.compareTo(activeAmount) >= 0) {
            order.setStatus("REFUNDED");
        } else if (refundedAmount.compareTo(BigDecimal.ZERO) > 0) {
            order.setStatus("PARTIALLY_REFUNDED");
        }
        paymentOrderMapper.updateById(order);
    }

    private void refreshRegistrationBatch(Long registrationBatchId) {
        if (registrationBatchId == null) {
            return;
        }
        RegistrationBatch batch = registrationBatchMapper.selectById(registrationBatchId);
        if (batch == null) {
            return;
        }
        List<BeerEntry> remaining = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getRegistrationBatchId, registrationBatchId));
        BigDecimal totalAmount = remaining.stream()
                .map(item -> findEntryPayment(item.getId()))
                .filter(Objects::nonNull)
                .map(EntryPayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        batch.setEntryCount(remaining.size());
        batch.setTotalAmount(totalAmount);
        if (remaining.isEmpty()) {
            batch.setStatus("CANCELED");
        }
        registrationBatchMapper.updateById(batch);
    }

    private String buildAdministrativeDeleteLogSummary(String reason, AdminEntryDeleteImpactVO impact) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", reason);
        payload.put("paymentStatus", impact.getPaymentStatus());
        payload.put("roundAssignments", impact.getRoundAssignmentCount());
        payload.put("scoreRecords", impact.getScoreRecordCount());
        payload.put("roundResults", impact.getRoundResultCount());
        payload.put("awards", impact.getAwardResultCount());
        payload.put("resultPublished", impact.getResultPublished());
        return writeObjectJson(payload, "保存删除记录失败");
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

    private EntryDelivery findEntryDelivery(Long beerEntryId) {
        return entryDeliveryMapper.selectOne(new LambdaQueryWrapper<EntryDelivery>()
                .eq(EntryDelivery::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
    }

    private BeerEntry requireEntry(Long entryId) {
        BeerEntry entry = beerEntryMapper.selectById(entryId);
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        competitionAccessService.requireCompetitionAccess(entry.getCompetitionId());
        return entry;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
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
