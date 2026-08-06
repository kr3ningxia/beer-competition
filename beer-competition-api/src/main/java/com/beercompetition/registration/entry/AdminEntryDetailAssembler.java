package com.beercompetition.registration.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.judging.scoring.EntryEvaluationDataService;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.PaymentOrderMapper;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.PaymentOrder;
import com.beercompetition.pojo.vo.AdminEntryDetailVO;
import com.beercompetition.pojo.vo.AdminEntryLogVO;
import com.beercompetition.pojo.vo.EntryDeliveryVO;
import com.beercompetition.pojo.vo.EntryExtraFieldVO;
import com.beercompetition.pojo.vo.EntryPaymentVO;
import com.beercompetition.pojo.vo.EntryRefundVO;
import com.beercompetition.pojo.vo.BankTransferVO;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.BankTransferPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 组装后台报名详情及其操作轨迹。
 */
@Service
@RequiredArgsConstructor
public class AdminEntryDetailAssembler {

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

    private static final String TARGET_ENTRY = "BEER_ENTRY";

    private final AdminOperationLogMapper adminOperationLogMapper;

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final EntryPaymentMapper entryPaymentMapper;

    private final EntryRefundMapper entryRefundMapper;

    private final PaymentOrderMapper paymentOrderMapper;

    private final EntryDeliveryMapper entryDeliveryMapper;

    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;

    private final BreweryMapper breweryMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final BankTransferPaymentService bankTransferPaymentService;

    private final EntryEvaluationDataService entryEvaluationDataService;

    public AdminEntryDetailVO toAdminEntryDetailVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        Brewery brewery = breweryMapper.selectById(entry.getBreweryId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryPayment payment = findEntryPayment(entry.getId());
        EntryDelivery delivery = findEntryDelivery(entry.getId());
        EntryRefund refund = findLatestRefund(entry.getId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        boolean resultPublished = isResultPublished(competition, entry);
        EntryEvaluationDataService.EntryEvaluationData evaluationData =
                entryEvaluationDataService.loadEntryEvaluationData(entry.getId());
        boolean assigned = evaluationData.assigned();
        BankTransferVO bankTransfer = resolveBankTransfer(payment);
        return AdminEntryDetailVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .competitionId(entry.getCompetitionId())
                .competitionName(competition == null ? null : competition.getName())
                .competitionCode(competition == null ? null : competition.getCode())
                .name(entry.getName())
                .breweryCompanyName(brewery == null ? null : brewery.getCompanyName())
                .breweryContactName(brewery == null ? null : brewery.getContactName())
                .categoryId(entry.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .abv(entry.getAbv())
                .status(entry.getStatus())
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .payment(toEntryPaymentVO(payment, competition))
                .bankTransfer(bankTransfer)
                .refund(toEntryRefundVO(refund))
                .offlineRefundAccountName(refund == null ? null : refund.getOfflineRefundAccountName())
                .offlineRefundBankName(refund == null ? null : refund.getOfflineRefundBankName())
                .offlineRefundAccountNoLast4(refund == null ? null : refund.getOfflineRefundAccountNoLast4())
                .offlineRefundTransferNo(refund == null ? null : refund.getOfflineRefundTransferNo())
                .offlineRefundTime(refund == null ? null : refund.getOfflineRefundTime())
                .offlineRefundVoucherAssetId(refund == null ? null : refund.getOfflineRefundVoucherAssetId())
                .refundStatus(refund == null ? null : refund.getStatus())
                .refundReason(refund == null ? null : refund.getReason())
                .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .delivery(toEntryDeliveryVO(delivery))
                .stored(Objects.equals(entry.getStoredFlag(), 1))
                .assigned(assigned)
                .resultPublished(resultPublished)
                .canConfirmPayment(canConfirmPayment(entry, payment))
                .canMarkStored(EntryStatus.REGISTERED.name().equals(entry.getStatus()) && !isActiveRefund(refund))
                .canUnmarkStored(canUnmarkStored(entry, competition, refund, evaluationData))
                .canCancel(canCancelEntry(entry, payment))
                .canApproveRefund(canApproveRefund(refund))
                .canRejectRefund(canRejectRefund(refund))
                .canRetryRefund(canRetryRefund(refund, payment))
                .canConfirmOfflineRefund(canConfirmOfflineRefund(refund, payment))
                .canEdit(!EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus()) && !resultPublished)
                .submittedAt(entry.getCreateTime())
                .extraFields(listExtraFields(entry.getId()))
                .traces(evaluationData.traces())
                .logs(listEntryLogs(entry.getUuid()))
                .build();
    }

    private List<EntryExtraFieldVO> listExtraFields(Long beerEntryId) {
        return beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .eq(BeerEntryExtraField::getBeerEntryId, beerEntryId))
                .stream()
                .map(this::toEntryExtraFieldVO)
                .toList();
    }

    private List<AdminEntryLogVO> listEntryLogs(String uuid) {
        return adminOperationLogMapper.selectList(new LambdaQueryWrapper<AdminOperationLog>()
                        .eq(AdminOperationLog::getTargetType, TARGET_ENTRY)
                        .eq(AdminOperationLog::getTargetPublicId, uuid)
                        .orderByDesc(AdminOperationLog::getId))
                .stream()
                .map(item -> AdminEntryLogVO.builder()
                        .id(item.getId())
                        .adminUserId(item.getAdminUserId())
                        .action(item.getAction())
                        .summary(item.getSummary())
                        .createTime(item.getCreateTime())
                        .build())
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

    private boolean canApproveRefund(EntryRefund refund) {
        return refund != null && EntryRefundStatus.REQUESTED.name().equals(refund.getStatus());
    }

    private boolean canRejectRefund(EntryRefund refund) {
        return refund != null && EntryRefundStatus.REQUESTED.name().equals(refund.getStatus());
    }

    private boolean canRetryRefund(EntryRefund refund, EntryPayment payment) {
        return refund != null && EntryRefundStatus.FAILED.name().equals(refund.getStatus())
                && !isManualRefundPayment(payment);
    }

    private boolean canConfirmOfflineRefund(EntryRefund refund, EntryPayment payment) {
        return refund != null && EntryRefundStatus.PROCESSING.name().equals(refund.getStatus())
                && isManualRefundPayment(payment);
    }

    private boolean canUnmarkStored(BeerEntry entry,
                                    Competition competition,
                                    EntryRefund refund,
                                    EntryEvaluationDataService.EntryEvaluationData evaluationData) {
        if (entry == null || !EntryStatus.STORED.name().equals(entry.getStatus()) || !Objects.equals(entry.getStoredFlag(), 1)) {
            return false;
        }
        if (isActiveRefund(refund) || isResultPublished(competition, entry)) {
            return false;
        }
        return !evaluationData.assigned()
                && !evaluationData.hasScoreRecord()
                && !evaluationData.hasRoundResult()
                && !evaluationData.hasAwardResult();
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

    private boolean isManualRefundPayment(EntryPayment payment) {
        return payment != null && (EntryPayMethod.BANK_TRANSFER.name().equals(payment.getPayMethod())
                || EntryPayMethod.MANUAL.name().equals(payment.getPayMethod()));
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

    private BankTransferVO resolveBankTransfer(EntryPayment payment) {
        if (payment == null) {
            return null;
        }
        Long transferId = payment.getBankTransferId();
        if (transferId == null && payment.getPaymentOrderId() != null) {
            PaymentOrder order = paymentOrderMapper.selectById(payment.getPaymentOrderId());
            transferId = order == null ? null : order.getBankTransferId();
        }
        return transferId == null ? null : bankTransferPaymentService.getAdminTransfer(transferId);
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

    private boolean canConfirmPayment(BeerEntry entry, EntryPayment payment) {
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            return false;
        }
        return payment == null || EntryPaymentStatus.UNPAID.name().equals(payment.getStatus());
    }

    private boolean canCancelEntry(BeerEntry entry, EntryPayment payment) {
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            return false;
        }
        return payment == null || !EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus());
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
