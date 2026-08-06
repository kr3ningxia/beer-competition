package com.beercompetition.registration.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.PaymentOrderMapper;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.LogisticsVisibility;
import com.beercompetition.pojo.enums.RefundApprovalMode;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.PaymentOrder;
import com.beercompetition.pojo.vo.EntryDeliveryVO;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntryExtraFieldVO;
import com.beercompetition.pojo.vo.EntryPaymentVO;
import com.beercompetition.pojo.vo.EntryRefundVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.CompetitionLogisticsVO;
import com.beercompetition.service.EntryScanLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 批量聚合报名关联数据并组装厂商端报名视图。
 */
@Service
@RequiredArgsConstructor
public class PortalEntryViewAssembler {

    private static final Set<String> LABEL_ALLOWED_STATUSES = Set.of(
                EntryStatus.REGISTERED.name(),
                EntryStatus.STORED.name(),
                EntryStatus.RESULT_PUBLISHED.name()
        );

    private static final Set<String> ACTIVE_REFUND_STATUSES = Set.of(
                EntryRefundStatus.REQUESTED.name(),
                EntryRefundStatus.APPROVED.name(),
                EntryRefundStatus.PROCESSING.name()
        );

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final EntryPaymentMapper entryPaymentMapper;

    private final EntryRefundMapper entryRefundMapper;

    private final PaymentOrderMapper paymentOrderMapper;

    private final EntryDeliveryMapper entryDeliveryMapper;

    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final PortalEntryEditPolicy portalEntryEditPolicy;

    public EntryDetailVO toEntryDetailVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryPayment payment = findEntryPayment(entry.getId());
        PaymentOrder paymentOrder = findPaymentOrder(payment);
        EntryDelivery delivery = findEntryDelivery(entry.getId());
        EntryRefund refund = findLatestRefund(entry.getId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        boolean activeRefund = isActiveRefund(refund);
        boolean resultPublished = isResultPublished(competition, entry);
        PortalEntryEditPolicy.EditDecision editDecision =
                portalEntryEditPolicy.evaluate(entry, competition, refund, resultPublished);
        return EntryDetailVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .competitionId(entry.getCompetitionId())
                .registrationBatchId(entry.getRegistrationBatchId())
                .paymentOrderId(payment == null ? null : payment.getPaymentOrderId())
                .paymentOrderStatus(paymentOrder == null ? null : paymentOrder.getStatus())
                .competitionType(resolveCompetitionType(competition).name())
                .competitionCode(competition == null ? null : competition.getCode())
                .name(entry.getName())
                .style(entry.getStyle())
                .abv(entry.getAbv())
                .categoryId(entry.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .competitionName(competition == null ? null : competition.getName())
                .competitionDate(competition == null ? null : competition.getCompetitionDate())
                .competitionLogistics(toEntryCompetitionLogisticsVO(competition, entry, payment))
                .status(entry.getStatus())
                .published(resultPublished)
                .entryFee(competition == null ? null : competition.getEntryFee())
                .storedFlag(entry.getStoredFlag())
                .stored(Objects.equals(entry.getStoredFlag(), 1))
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .payment(toEntryPaymentVO(payment, competition))
                .refund(toEntryRefundVO(refund))
                .refundStatus(refund == null ? null : refund.getStatus())
                .refundReason(refund == null ? null : refund.getReason())
                .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                .refundApprovalMode(resolveRefundApprovalMode(competition).name())
                .canRequestRefund(canRequestRefund(entry, competition, payment, refund))
                .canUpdateInfo(editDecision.allowed())
                .updateInfoDisabledReason(editDecision.disabledReason())
                .delivery(toEntryDeliveryVO(delivery))
                .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .deliveryNote(delivery == null ? null : delivery.getDeliveryNote())
                .deliverySubmittedAt(delivery == null ? null : delivery.getSubmittedTime())
                .deliveryReceivedAt(delivery == null ? null : delivery.getReceivedTime())
                .canDownloadLabel(LABEL_ALLOWED_STATUSES.contains(entry.getStatus()) && !activeRefund)
                .submittedAt(entry.getCreateTime())
                .extraFields(listExtraFields(entry.getId()))
                .build();
    }

    public EntrySummaryVO toEntrySummaryVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryPayment payment = findEntryPayment(entry.getId());
        PaymentOrder paymentOrder = findPaymentOrder(payment);
        EntryDelivery delivery = findEntryDelivery(entry.getId());
        EntryRefund refund = findLatestRefund(entry.getId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        boolean activeRefund = isActiveRefund(refund);
        boolean resultPublished = isResultPublished(competition, entry);
        PortalEntryEditPolicy.EditDecision editDecision =
                portalEntryEditPolicy.evaluate(entry, competition, refund, resultPublished);
        return EntrySummaryVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .competitionId(entry.getCompetitionId())
                .registrationBatchId(entry.getRegistrationBatchId())
                .paymentOrderId(payment == null ? null : payment.getPaymentOrderId())
                .paymentOrderStatus(paymentOrder == null ? null : paymentOrder.getStatus())
                .competitionType(resolveCompetitionType(competition).name())
                .competitionCode(competition == null ? null : competition.getCode())
                .name(entry.getName())
                .competitionName(competition == null ? null : competition.getName())
                .competitionLogistics(toEntryCompetitionLogisticsVO(competition, entry, payment))
                .categoryId(entry.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .status(entry.getStatus())
                .published(resultPublished)
                .abv(entry.getAbv())
                .entryFee(competition == null ? null : competition.getEntryFee())
                .storedFlag(entry.getStoredFlag())
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .payment(toEntryPaymentVO(payment, competition))
                .refund(toEntryRefundVO(refund))
                .refundStatus(refund == null ? null : refund.getStatus())
                .refundReason(refund == null ? null : refund.getReason())
                .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                .refundApprovalMode(resolveRefundApprovalMode(competition).name())
                .canRequestRefund(canRequestRefund(entry, competition, payment, refund))
                .canUpdateInfo(editDecision.allowed())
                .updateInfoDisabledReason(editDecision.disabledReason())
                .delivery(toEntryDeliveryVO(delivery))
                .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .canDownloadLabel(LABEL_ALLOWED_STATUSES.contains(entry.getStatus()) && !activeRefund)
                .submittedAt(entry.getCreateTime())
                .build();
    }

    private CompetitionLogisticsVO toEntryCompetitionLogisticsVO(Competition competition, BeerEntry entry, EntryPayment payment) {
        if (competition == null) {
            return null;
        }
        CompetitionLogisticsVO logistics = CompetitionLogisticsVO.builder()
                .deliveryMethod(competition.getDeliveryMethod())
                .sampleArrivalStart(competition.getSampleArrivalStart())
                .sampleArrivalDeadline(competition.getSampleArrivalDeadline())
                .sampleQuantityNote(competition.getSampleQuantityNote())
                .deliveryRecipient(competition.getDeliveryRecipient())
                .deliveryPhone(competition.getDeliveryPhone())
                .deliveryAddress(competition.getDeliveryAddress())
                .deliveryNote(competition.getDeliveryNote())
                .logisticsVisibility(competition.getLogisticsVisibility())
                .build();
        if (canViewFullLogistics(logistics, entry, payment)) {
            return logistics;
        }
        logistics.setDeliveryRecipient(null);
        logistics.setDeliveryPhone(null);
        logistics.setDeliveryAddress(null);
        return logistics;
    }

    private boolean canViewFullLogistics(CompetitionLogisticsVO logistics, BeerEntry entry, EntryPayment payment) {
        if (logistics == null || !StringUtils.hasText(logistics.getLogisticsVisibility())) {
            return false;
        }
        if (LogisticsVisibility.PUBLIC.name().equals(logistics.getLogisticsVisibility())) {
            return true;
        }
        if (LogisticsVisibility.LOGIN_REQUIRED.name().equals(logistics.getLogisticsVisibility())) {
            return true;
        }
        return EntryPaymentStatus.PAID.name().equals(resolvePaymentStatus(entry, payment))
                || LABEL_ALLOWED_STATUSES.contains(entry.getStatus());
    }

    private List<EntryExtraFieldVO> listExtraFields(Long beerEntryId) {
        return beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .eq(BeerEntryExtraField::getBeerEntryId, beerEntryId))
                .stream()
                .map(this::toEntryExtraFieldVO)
                .toList();
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

    private boolean isActiveRefund(EntryRefund refund) {
        return refund != null && ACTIVE_REFUND_STATUSES.contains(refund.getStatus());
    }

    private boolean canRequestRefund(BeerEntry entry, Competition competition, EntryPayment payment,
                                     EntryRefund refund) {
        if (entry == null || competition == null || payment == null) {
            return false;
        }
        if (!LABEL_ALLOWED_STATUSES.contains(entry.getStatus())) {
            return false;
        }
        if (!EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            return false;
        }
        if (competition.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(competition.getRegistrationDeadline())) {
            return false;
        }
        return refund == null || EntryRefundStatus.REJECTED.name().equals(refund.getStatus());
    }

    private RefundApprovalMode resolveRefundApprovalMode(Competition competition) {
        return RefundApprovalMode.of(competition == null ? null : competition.getRefundApprovalMode());
    }

    private EntryDelivery findEntryDelivery(Long beerEntryId) {
        return entryDeliveryMapper.selectOne(new LambdaQueryWrapper<EntryDelivery>()
                .eq(EntryDelivery::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
    }

    private EntryExtraFieldVO toEntryExtraFieldVO(BeerEntryExtraField item) {
        return EntryExtraFieldVO.builder()
                .key(item.getFieldKey())
                .label(item.getFieldLabel())
                .value(item.getFieldValue())
                .build();
    }

    private EntryPaymentVO toEntryPaymentVO(EntryPayment payment, Competition competition) {
        return EntryPaymentVO.builder()
                .paymentOrderId(payment == null ? null : payment.getPaymentOrderId())
                .status(payment == null ? EntryPaymentStatus.UNPAID.name() : payment.getStatus())
                .payMethod(resolvePayMethod(payment == null ? null : payment.getPayMethod()))
                .amount(payment == null ? competition == null ? null : competition.getEntryFee() : payment.getAmount())
                .outTradeNo(payment == null ? null : payment.getOutTradeNo())
                .wechatTransactionId(payment == null ? null : payment.getWechatTransactionId())
                .bankTransferId(payment == null ? null : payment.getBankTransferId())
                .codeUrl(payment == null ? null : payment.getCodeUrl())
                .expireTime(payment == null ? null : payment.getExpireTime())
                .paidAmount(payment == null ? null : payment.getPaidAmount())
                .wechatTradeState(payment == null ? null : payment.getWechatTradeState())
                .wechatTradeStateDesc(payment == null ? null : payment.getWechatTradeStateDesc())
                .paidTime(payment == null ? null : payment.getPaidTime())
                .build();
    }

    private PaymentOrder findPaymentOrder(EntryPayment payment) {
        if (payment == null || payment.getPaymentOrderId() == null) {
            return null;
        }
        return paymentOrderMapper.selectById(payment.getPaymentOrderId());
    }

    private EntryRefundVO toEntryRefundVO(EntryRefund refund) {
        if (refund == null) {
            return null;
        }
        return EntryRefundVO.builder()
                .id(refund.getId())
                .beerEntryId(refund.getBeerEntryId())
                .entryPaymentId(refund.getEntryPaymentId())
                .refundNo(refund.getRefundNo())
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .approvalModeSnapshot(refund.getApprovalModeSnapshot())
                .reason(refund.getReason())
                .requestedByPortalId(refund.getRequestedByPortalId())
                .requestedTime(refund.getRequestedTime())
                .processedByAdminId(refund.getProcessedByAdminId())
                .processedTime(refund.getProcessedTime())
                .successTime(refund.getSuccessTime())
                .failReason(refund.getFailReason())
                .wechatRefundId(refund.getWechatRefundId())
                .wechatRefundStatus(refund.getWechatRefundStatus())
                .outRefundNo(refund.getOutRefundNo())
                .build();
    }

    private EntryDeliveryVO toEntryDeliveryVO(EntryDelivery delivery) {
        return EntryDeliveryVO.builder()
                .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .deliveryNote(delivery == null ? null : delivery.getDeliveryNote())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .submittedTime(delivery == null ? null : delivery.getSubmittedTime())
                .receivedTime(delivery == null ? null : delivery.getReceivedTime())
                .build();
    }

    private boolean isResultPublished(Competition competition, BeerEntry entry) {
        return EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus())
                || (competition != null && CompetitionStatus.PUBLISHED.name().equals(competition.getStatus()));
    }

    private CompetitionType resolveCompetitionType(Competition competition) {
        return CompetitionType.of(competition == null ? null : competition.getCompetitionType());
    }

    private String resolvePaymentStatus(BeerEntry entry, EntryPayment payment) {
        if (payment != null && StringUtils.hasText(payment.getStatus())) {
            return payment.getStatus();
        }
        if (EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            return EntryPaymentStatus.UNPAID.name();
        }
        return LABEL_ALLOWED_STATUSES.contains(entry.getStatus())
                ? EntryPaymentStatus.PAID.name()
                : EntryPaymentStatus.UNPAID.name();
    }

    private String resolveDeliveryStatus(EntryDelivery delivery) {
        if (delivery == null || !StringUtils.hasText(delivery.getDeliveryStatus())) {
            return EntryDeliveryStatus.NOT_SUBMITTED.name();
        }
        return delivery.getDeliveryStatus();
    }

    private String resolvePayMethod(String payMethod) {
        return StringUtils.hasText(payMethod) ? payMethod : EntryPayMethod.MANUAL.name();
    }
}
