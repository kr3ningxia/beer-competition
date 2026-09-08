package com.beercompetition.registration.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.judging.scoring.EntryEvaluationDataService;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.vo.AdminEntryTraceVO;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.beercompetition.service.EntryScanLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组装后台报名列表和退款列表使用的摘要视图。
 */
@Service
@RequiredArgsConstructor
public class AdminEntryListAssembler {

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

    private final EntryDeliveryMapper entryDeliveryMapper;

    private final BreweryMapper breweryMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final EntryEvaluationDataService entryEvaluationDataService;

    public AdminEntryVO toAdminEntryVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        Brewery brewery = breweryMapper.selectById(entry.getBreweryId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryPayment payment = findEntryPayment(entry.getId());
        EntryDelivery delivery = findEntryDelivery(entry.getId());
        EntryRefund refund = findLatestRefund(entry.getId());
        EntryScanLabel label = entryScanLabelService.requireLatestLabel(entry.getId());
        EntryEvaluationDataService.EntryEvaluationData evaluationData =
                entryEvaluationDataService.loadEntryEvaluationData(entry.getId());
        List<AdminEntryTraceVO> traces = evaluationData.traces();
        boolean assigned = evaluationData.assigned();
        boolean resultPublished = isResultPublished(competition, entry);
        return AdminEntryVO.builder()
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
                .boxNumber(entry.getBoxNumber())
                .abv(entry.getAbv())
                .status(entry.getStatus())
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .payMethod(payment == null ? null : payment.getPayMethod())
                .refundStatus(refund == null ? null : refund.getStatus())
                .refundReason(refund == null ? null : refund.getReason())
                .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .stored(Objects.equals(entry.getStoredFlag(), 1))
                .assigned(assigned)
                .pathText(formatEntryPathText(traces))
                .submittedAt(entry.getCreateTime())
                .paidTime(payment == null ? null : payment.getPaidTime())
                .deliveryReceivedAt(delivery == null ? null : delivery.getReceivedTime())
                .lastModifiedAt(resolveEntryLastModifiedAt(entry, payment, delivery, refund))
                .canConfirmPayment(canConfirmPayment(entry, payment))
                .canMarkStored(EntryStatus.REGISTERED.name().equals(entry.getStatus()) && !isActiveRefund(refund))
                .canUnmarkStored(canUnmarkStored(entry, competition, refund, evaluationData))
                .canCancel(canCancelEntry(entry, payment))
                .canApproveRefund(canApproveRefund(refund))
                .canRejectRefund(canRejectRefund(refund))
                .canRetryRefund(canRetryRefund(refund, payment))
                .canConfirmOfflineRefund(canConfirmOfflineRefund(refund, payment))
                .canEdit(!EntryStatus.CANCELED.name().equals(entry.getStatus())
                        && !EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus()) && !resultPublished)
                .traces(traces)
                .build();
    }

    private String formatEntryPathText(List<AdminEntryTraceVO> traces) {
        List<String> parts = traces.stream()
                .filter(item -> "ROUND".equals(item.getType()))
                .map(item -> {
                    List<String> labels = new ArrayList<>();
                    labels.add(item.getRoundName());
                    labels.add(item.getTableName());
                    return labels.stream()
                        .filter(StringUtils::hasText)
                        .collect(Collectors.joining(" "));
                })
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (parts.isEmpty()) {
            return "未分桌";
        }
        return String.join(" -> ", parts);
    }

    private LocalDateTime resolveEntryLastModifiedAt(BeerEntry entry, EntryPayment payment, EntryDelivery delivery) {
        return resolveEntryLastModifiedAt(entry, payment, delivery, null);
    }

    private LocalDateTime resolveEntryLastModifiedAt(BeerEntry entry, EntryPayment payment, EntryDelivery delivery,
                                                     EntryRefund refund) {
        List<LocalDateTime> times = new ArrayList<>();
        times.add(entry.getUpdateTime());
        times.add(payment == null ? null : payment.getUpdateTime());
        times.add(delivery == null ? null : delivery.getUpdateTime());
        times.add(refund == null ? null : refund.getUpdateTime());
        return times.stream()
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(entry.getCreateTime());
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
        if (refund == null || !isManualRefundPayment(payment)) {
            return false;
        }
        if (EntryPayMethod.WECHAT_QR.name().equals(payment.getPayMethod())) {
            return EntryRefundStatus.APPROVED.name().equals(refund.getStatus());
        }
        return EntryRefundStatus.PROCESSING.name().equals(refund.getStatus());
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

    private boolean isManualRefundPayment(EntryPayment payment) {
        return payment != null && (EntryPayMethod.BANK_TRANSFER.name().equals(payment.getPayMethod())
                || EntryPayMethod.MANUAL.name().equals(payment.getPayMethod())
                || EntryPayMethod.WECHAT_QR.name().equals(payment.getPayMethod()));
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
}
