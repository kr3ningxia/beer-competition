package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.WechatPayNotifyMapper;
import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.properties.WechatPayProperties;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.WechatPayNotify;
import com.beercompetition.pojo.vo.EntryPaymentStatusVO;
import com.beercompetition.pojo.vo.WechatNativePayVO;
import com.beercompetition.service.WechatPaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WechatPaymentServiceImpl implements WechatPaymentService {

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
    private static final String BUSINESS_PAYMENT = "PAYMENT";
    private static final String BUSINESS_REFUND = "REFUND";
    private static final int NATIVE_PAY_EXPIRE_MINUTES = 30;

    private final WechatPayClient wechatPayClient;
    private final WechatPayProperties wechatPayProperties;
    private final PortalAccountMapper portalAccountMapper;
    private final BreweryMapper breweryMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final CompetitionMapper competitionMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final EntryRefundMapper entryRefundMapper;
    private final WechatPayNotifyMapper wechatPayNotifyMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    public WechatNativePayVO createNativePayment(Long entryId) {
        // 1) 校验厂牌作品与支付资格
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        EntryPayment payment = ensurePayment(entry);
        if (EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            return toNativePayVO(payment);
        }
        if (EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus())) {
            throw new BaseException("银行转账信息已提交，请等待组委会核对到账");
        }
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            throw new BaseException("当前酒款不能支付报名费");
        }
        if (hasActiveRefund(entry.getId())) {
            throw new BaseException("退款处理中，不能重新支付");
        }

        // 2) 复用未过期二维码或处理过期订单
        LocalDateTime now = LocalDateTime.now();
        if (canReusePaymentQr(payment, now)) {
            return toNativePayVO(payment);
        }
        if (StringUtils.hasText(payment.getOutTradeNo()) && payment.getExpireTime() != null
                && !EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            syncExpiredPayment(payment, entry);
            payment = entryPaymentMapper.selectById(payment.getId());
            if (EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
                return toNativePayVO(payment);
            }
        }

        // 3) 调用微信 Native 下单并保存二维码
        String outTradeNo = generateOutTradeNo();
        LocalDateTime expireTime = now.plusMinutes(NATIVE_PAY_EXPIRE_MINUTES);
        WechatPayClient.NativePayResult result = wechatPayClient.createNativePayment(
                new WechatPayClient.NativePayRequest(outTradeNo, buildPaymentDescription(entry), payment.getAmount(), expireTime));

        payment.setPayMethod(EntryPayMethod.WECHAT.name());
        payment.setStatus(EntryPaymentStatus.UNPAID.name());
        payment.setOutTradeNo(outTradeNo);
        payment.setCodeUrl(result.codeUrl());
        payment.setExpireTime(expireTime);
        payment.setWechatTradeState("NOTPAY");
        payment.setWechatTradeStateDesc("待支付");
        entryPaymentMapper.updateById(payment);
        return toNativePayVO(payment);
    }

    @Override
    public EntryPaymentStatusVO getPortalPaymentStatus(Long entryId) {
        // 1) 校验厂牌作品
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        EntryPayment payment = findPayment(entry.getId());

        // 2) 必要时主动查单补偿
        if (payment != null && shouldQueryPayment(payment)) {
            syncPaymentQuery(payment, entry);
            payment = findPayment(entry.getId());
            entry = requireEntry(entryId);
        }

        // 3) 返回状态
        return EntryPaymentStatusVO.builder()
                .entryId(entry.getId())
                .entryStatus(entry.getStatus())
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .canDownloadLabel(canDownloadLabel(entry))
                .paidTime(payment == null ? null : payment.getPaidTime())
                .build();
    }

    @Override
    public void handlePaymentNotify(WechatPayClient.WechatNotifyRequest request) {
        WechatPayClient.PaymentNotifyResult result = wechatPayClient.parsePaymentNotify(request);
        if (isDuplicateNotify(result.notifyId())) {
            return;
        }
        WechatPayNotify notify = insertNotify(result.notifyId(), result.eventType(), BUSINESS_PAYMENT,
                result.outTradeNo(), null, result.transactionId(), null, result.rawJson());
        try {
            transactionTemplate.executeWithoutResult(status -> applyPaymentSuccess(result));
            markNotifyProcessed(notify.getId(), "OK");
        } catch (RuntimeException ex) {
            markNotifyProcessed(notify.getId(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public void handleRefundNotify(WechatPayClient.WechatNotifyRequest request) {
        WechatPayClient.RefundNotifyResult result = wechatPayClient.parseRefundNotify(request);
        if (isDuplicateNotify(result.notifyId())) {
            return;
        }
        WechatPayNotify notify = insertNotify(result.notifyId(), result.eventType(), BUSINESS_REFUND,
                result.outTradeNo(), result.outRefundNo(), null, result.refundId(), result.rawJson());
        try {
            transactionTemplate.executeWithoutResult(status -> applyRefundResult(result.outRefundNo(), result.refundId(),
                    result.refundStatus(), result.successTime(), result.rawJson()));
            markNotifyProcessed(notify.getId(), "OK");
        } catch (RuntimeException ex) {
            markNotifyProcessed(notify.getId(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public void approveRefund(Long refundId, String reason, Long adminId) {
        // 1) 查询退款上下文并按付款方式分流
        RefundContext context = transactionTemplate.execute(status -> prepareRefund(refundId, reason, adminId));
        if (isManualRefundPayment(context.payment())) {
            transactionTemplate.executeWithoutResult(status -> applyManualRefundSuccess(context.refund().getId(), adminId));
            return;
        }

        // 2) 微信付款发起原路退款
        WechatPayClient.RefundResult result = wechatPayClient.createRefund(new WechatPayClient.RefundRequest(
                context.payment().getOutTradeNo(),
                context.payment().getWechatTransactionId(),
                context.refund().getOutRefundNo(),
                context.refund().getReason(),
                context.refund().getAmount(),
                context.payment().getAmount()
        ));

        // 3) 应用微信退款受理结果
        transactionTemplate.executeWithoutResult(status -> applyRefundResult(result.outRefundNo(), result.refundId(),
                result.refundStatus(), result.successTime(), null));
    }

    @Override
    public void autoApproveWechatRefund(Long refundId, String reason) {
        // 1) 自动受理仅处理微信支付退款
        RefundContext context = transactionTemplate.execute(status -> prepareWechatAutoRefund(refundId, reason));
        if (context == null) {
            return;
        }

        // 2) 调用微信原路退款，失败时登记为可后台重试
        try {
            WechatPayClient.RefundResult result = wechatPayClient.createRefund(new WechatPayClient.RefundRequest(
                    context.payment().getOutTradeNo(),
                    context.payment().getWechatTransactionId(),
                    context.refund().getOutRefundNo(),
                    context.refund().getReason(),
                    context.refund().getAmount(),
                    context.payment().getAmount()
            ));
            transactionTemplate.executeWithoutResult(status -> applyRefundResult(result.outRefundNo(), result.refundId(),
                    result.refundStatus(), result.successTime(), null));
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markAutoRefundFailed(refundId, ex));
        }
    }

    @Override
    public void retryRefund(Long refundId, String reason, Long adminId) {
        EntryRefund refund = requireRefund(refundId);
        if (!EntryRefundStatus.FAILED.name().equals(refund.getStatus())) {
            throw new BaseException("只有退款失败记录可以重试");
        }
        EntryPayment payment = entryPaymentMapper.selectById(refund.getEntryPaymentId());
        if (isManualRefundPayment(payment)) {
            throw new BaseException("线下退款请确认完成后直接登记，不能重试微信退款");
        }
        approveRefund(refundId, reason, adminId);
    }

    private void syncExpiredPayment(EntryPayment payment, BeerEntry entry) {
        try {
            syncPaymentQuery(payment, entry);
            EntryPayment refreshed = entryPaymentMapper.selectById(payment.getId());
            if (!EntryPaymentStatus.PAID.name().equals(refreshed.getStatus())) {
                wechatPayClient.closePayment(payment.getOutTradeNo());
            }
        } catch (Exception ex) {
            throw new BaseException("支付订单状态同步失败，请稍后重试");
        }
    }

    private void syncPaymentQuery(EntryPayment payment, BeerEntry entry) {
        WechatPayClient.PaymentQueryResult result = wechatPayClient.queryPayment(payment.getOutTradeNo());
        payment.setWechatTransactionId(result.transactionId());
        payment.setWechatTradeState(result.tradeState());
        payment.setWechatTradeStateDesc(result.tradeStateDesc());
        payment.setPaidAmount(result.paidAmount());
        payment.setPaidTime(result.paidTime());
        payment.setLastQueryTime(LocalDateTime.now());
        if ("SUCCESS".equals(result.tradeState())) {
            transactionTemplate.executeWithoutResult(status -> applyPaymentSuccess(
                    new WechatPayClient.PaymentNotifyResult(
                            "QUERY-" + payment.getOutTradeNo(),
                            "QUERY.SUCCESS",
                            payment.getOutTradeNo(),
                            result.transactionId(),
                            result.tradeState(),
                            result.tradeStateDesc(),
                            result.paidAmount(),
                            result.paidTime(),
                            null
                    )));
        } else {
            entryPaymentMapper.updateById(payment);
        }
    }

    private void applyPaymentSuccess(WechatPayClient.PaymentNotifyResult result) {
        EntryPayment payment = entryPaymentMapper.selectOne(new LambdaQueryWrapper<EntryPayment>()
                .eq(EntryPayment::getOutTradeNo, result.outTradeNo()));
        if (payment == null) {
            throw new ResourceNotFoundException("支付订单不存在");
        }
        BeerEntry entry = requireEntry(payment.getBeerEntryId());
        if (EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            return;
        }
        if (!"SUCCESS".equals(result.tradeState())) {
            throw new BaseException("微信支付未成功");
        }
        if (result.paidAmount() != null && payment.getAmount().compareTo(result.paidAmount()) != 0) {
            throw new BaseException("微信支付金额不一致");
        }
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            throw new BaseException("当前报名状态不能确认支付");
        }

        payment.setStatus(EntryPaymentStatus.PAID.name());
        payment.setPayMethod(EntryPayMethod.WECHAT.name());
        payment.setWechatTransactionId(result.transactionId());
        payment.setPaidAmount(result.paidAmount() == null ? payment.getAmount() : result.paidAmount());
        payment.setWechatTradeState(result.tradeState());
        payment.setWechatTradeStateDesc(result.tradeStateDesc());
        payment.setNotifyRawJson(result.rawJson());
        payment.setPaidTime(result.paidTime() == null ? LocalDateTime.now() : result.paidTime());
        entryPaymentMapper.updateById(payment);

        entry.setStatus(EntryStatus.REGISTERED.name());
        beerEntryMapper.updateById(entry);
    }

    private RefundContext prepareRefund(Long refundId, String reason, Long adminId) {
        EntryRefund refund = requireRefund(refundId);
        if (!EntryRefundStatus.REQUESTED.name().equals(refund.getStatus())
                && !EntryRefundStatus.FAILED.name().equals(refund.getStatus())) {
            throw new BaseException("当前退款状态不能确认退款");
        }
        BeerEntry entry = requireEntry(refund.getBeerEntryId());
        EntryPayment payment = findPayment(entry.getId());
        if (payment == null || !EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            throw new BaseException("只有已支付报名可以退款");
        }
        if (!isManualRefundPayment(payment)) {
            if (!StringUtils.hasText(payment.getOutTradeNo()) && wechatPayProperties.isWechatMode()) {
                throw new BaseException("缺少支付订单号，无法发起微信退款");
            }
            if (!StringUtils.hasText(payment.getOutTradeNo())) {
                payment.setOutTradeNo(generateOutTradeNo());
                entryPaymentMapper.updateById(payment);
            }
            if (!StringUtils.hasText(refund.getOutRefundNo())) {
                refund.setOutRefundNo(generateOutRefundNo());
            }
        }
        refund.setStatus(EntryRefundStatus.PROCESSING.name());
        refund.setProcessedByAdminId(adminId);
        refund.setProcessedTime(LocalDateTime.now());
        refund.setFailReason(null);
        entryRefundMapper.updateById(refund);
        writeEntryLog(adminId, "ENTRY_REFUND_APPROVE", entry.getUuid(), buildStatusLogSummary("受理退款", reason));
        return new RefundContext(refund, payment, entry);
    }

    private RefundContext prepareWechatAutoRefund(Long refundId, String reason) {
        EntryRefund refund = requireRefund(refundId);
        if (!EntryRefundStatus.REQUESTED.name().equals(refund.getStatus())) {
            throw new BaseException("当前退款状态不能自动受理");
        }
        BeerEntry entry = requireEntry(refund.getBeerEntryId());
        EntryPayment payment = findPayment(entry.getId());
        if (payment == null || !EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            throw new BaseException("只有已支付报名可以退款");
        }
        if (!EntryPayMethod.WECHAT.name().equals(payment.getPayMethod())) {
            throw new BaseException("只有微信支付可以自动退款");
        }
        if (!StringUtils.hasText(payment.getOutTradeNo())) {
            throw new BaseException("缺少支付订单号，无法发起微信退款");
        }
        if (!StringUtils.hasText(refund.getOutRefundNo())) {
            refund.setOutRefundNo(generateOutRefundNo());
        }
        refund.setStatus(EntryRefundStatus.PROCESSING.name());
        refund.setProcessedByAdminId(null);
        refund.setProcessedTime(LocalDateTime.now());
        refund.setFailReason(null);
        entryRefundMapper.updateById(refund);
        return new RefundContext(refund, payment, entry);
    }

    private void markAutoRefundFailed(Long refundId, RuntimeException ex) {
        EntryRefund refund = requireRefund(refundId);
        if (EntryRefundStatus.SUCCESS.name().equals(refund.getStatus())) {
            return;
        }
        refund.setStatus(EntryRefundStatus.FAILED.name());
        refund.setFailReason(limitFailReason("微信退款发起失败：" + (ex.getMessage() == null ? "请稍后重试" : ex.getMessage())));
        refund.setLastQueryTime(LocalDateTime.now());
        entryRefundMapper.updateById(refund);
    }

    private void applyManualRefundSuccess(Long refundId, Long adminId) {
        EntryRefund refund = requireRefund(refundId);
        EntryPayment payment = entryPaymentMapper.selectById(refund.getEntryPaymentId());
        BeerEntry entry = requireEntry(refund.getBeerEntryId());

        refund.setStatus(EntryRefundStatus.SUCCESS.name());
        refund.setSuccessTime(LocalDateTime.now());
        refund.setFailReason(null);
        refund.setWechatRefundStatus("MANUAL_SUCCESS");
        refund.setLastQueryTime(LocalDateTime.now());
        entryRefundMapper.updateById(refund);

        payment.setStatus(EntryPaymentStatus.REFUNDED.name());
        entryPaymentMapper.updateById(payment);

        entry.setStatus(EntryStatus.CANCELED.name());
        beerEntryMapper.updateById(entry);
        writeEntryLog(adminId, "ENTRY_REFUND_SUCCESS", entry.getUuid(), buildStatusLogSummary("线下退款完成", null));
    }

    private void applyRefundResult(String outRefundNo, String refundId, String refundStatus,
                                   LocalDateTime successTime, String rawJson) {
        EntryRefund refund = entryRefundMapper.selectOne(new LambdaQueryWrapper<EntryRefund>()
                .eq(EntryRefund::getOutRefundNo, outRefundNo));
        if (refund == null) {
            throw new ResourceNotFoundException("退款单不存在");
        }
        EntryPayment payment = entryPaymentMapper.selectById(refund.getEntryPaymentId());
        BeerEntry entry = requireEntry(refund.getBeerEntryId());
        refund.setWechatRefundId(refundId);
        refund.setWechatRefundStatus(refundStatus);
        if (StringUtils.hasText(rawJson)) {
            refund.setRefundNotifyRawJson(rawJson);
        }
        refund.setLastQueryTime(LocalDateTime.now());

        if ("SUCCESS".equals(refundStatus)) {
            refund.setStatus(EntryRefundStatus.SUCCESS.name());
            refund.setSuccessTime(successTime == null ? LocalDateTime.now() : successTime);
            refund.setFailReason(null);
            entryRefundMapper.updateById(refund);

            payment.setStatus(EntryPaymentStatus.REFUNDED.name());
            entryPaymentMapper.updateById(payment);

            entry.setStatus(EntryStatus.CANCELED.name());
            beerEntryMapper.updateById(entry);
            writeEntryLog(refund.getProcessedByAdminId(), "ENTRY_REFUND_SUCCESS", entry.getUuid(), buildStatusLogSummary("退款完成", null));
            return;
        }

        if ("PROCESSING".equals(refundStatus)) {
            refund.setStatus(EntryRefundStatus.PROCESSING.name());
            entryRefundMapper.updateById(refund);
            return;
        }

        refund.setStatus(EntryRefundStatus.FAILED.name());
        refund.setFailReason("微信退款状态：" + (StringUtils.hasText(refundStatus) ? refundStatus : "UNKNOWN"));
        entryRefundMapper.updateById(refund);
    }

    private String limitFailReason(String failReason) {
        if (!StringUtils.hasText(failReason)) {
            return null;
        }
        return failReason.length() <= 300 ? failReason : failReason.substring(0, 300);
    }

    private boolean canReusePaymentQr(EntryPayment payment, LocalDateTime now) {
        return EntryPaymentStatus.UNPAID.name().equals(payment.getStatus())
                && StringUtils.hasText(payment.getCodeUrl())
                && payment.getExpireTime() != null
                && payment.getExpireTime().isAfter(now.plusSeconds(30));
    }

    private boolean shouldQueryPayment(EntryPayment payment) {
        if (!wechatPayProperties.isWechatMode()) {
            return false;
        }
        if (!EntryPaymentStatus.UNPAID.name().equals(payment.getStatus()) || !StringUtils.hasText(payment.getOutTradeNo())) {
            return false;
        }
        return payment.getLastQueryTime() == null || payment.getLastQueryTime().isBefore(LocalDateTime.now().minusSeconds(10));
    }

    private boolean isManualRefundPayment(EntryPayment payment) {
        return payment != null && (EntryPayMethod.BANK_TRANSFER.name().equals(payment.getPayMethod())
                || EntryPayMethod.MANUAL.name().equals(payment.getPayMethod()));
    }

    private WechatNativePayVO toNativePayVO(EntryPayment payment) {
        return WechatNativePayVO.builder()
                .mode(wechatPayProperties.normalizedMode())
                .outTradeNo(payment.getOutTradeNo())
                .amount(payment.getAmount())
                .codeUrl(payment.getCodeUrl())
                .expireTime(payment.getExpireTime())
                .paymentStatus(payment.getStatus())
                .build();
    }

    private EntryPayment ensurePayment(BeerEntry entry) {
        EntryPayment payment = findPayment(entry.getId());
        if (payment != null) {
            return payment;
        }
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        EntryPayment created = EntryPayment.builder()
                .beerEntryId(entry.getId())
                .amount(competition == null ? BigDecimal.ZERO : competition.getEntryFee())
                .status(EntryPaymentStatus.UNPAID.name())
                .payMethod(EntryPayMethod.WECHAT.name())
                .build();
        entryPaymentMapper.insert(created);
        return created;
    }

    private EntryPayment findPayment(Long beerEntryId) {
        return entryPaymentMapper.selectOne(new LambdaQueryWrapper<EntryPayment>()
                .eq(EntryPayment::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
    }

    private EntryRefund requireRefund(Long refundId) {
        EntryRefund refund = entryRefundMapper.selectById(refundId);
        if (refund == null) {
            throw new ResourceNotFoundException("退款申请不存在");
        }
        return refund;
    }

    private PortalAccount requirePortalAccount() {
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("厂牌账号不存在");
        }
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }
        return account;
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

    private boolean hasActiveRefund(Long entryId) {
        return entryRefundMapper.selectCount(new LambdaQueryWrapper<EntryRefund>()
                .eq(EntryRefund::getBeerEntryId, entryId)
                .in(EntryRefund::getStatus, ACTIVE_REFUND_STATUSES)) > 0;
    }

    private String resolvePaymentStatus(BeerEntry entry, EntryPayment payment) {
        if (payment != null && StringUtils.hasText(payment.getStatus())) {
            return payment.getStatus();
        }
        return EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())
                ? EntryPaymentStatus.UNPAID.name()
                : EntryPaymentStatus.PAID.name();
    }

    private boolean canDownloadLabel(BeerEntry entry) {
        return LABEL_ALLOWED_STATUSES.contains(entry.getStatus());
    }

    private boolean isDuplicateNotify(String notifyId) {
        return StringUtils.hasText(notifyId) && wechatPayNotifyMapper.selectCount(new LambdaQueryWrapper<WechatPayNotify>()
                .eq(WechatPayNotify::getNotifyId, notifyId)) > 0;
    }

    private WechatPayNotify insertNotify(String notifyId, String eventType, String businessType,
                                         String outTradeNo, String outRefundNo, String transactionId,
                                         String refundId, String rawJson) {
        WechatPayNotify notify = WechatPayNotify.builder()
                .notifyId(StringUtils.hasText(notifyId) ? notifyId : "LOCAL-" + UUID.randomUUID())
                .eventType(StringUtils.hasText(eventType) ? eventType : "UNKNOWN")
                .businessType(businessType)
                .outTradeNo(outTradeNo)
                .outRefundNo(outRefundNo)
                .wechatTransactionId(transactionId)
                .wechatRefundId(refundId)
                .rawJson(StringUtils.hasText(rawJson) ? rawJson : "{}")
                .processedFlag(0)
                .build();
        wechatPayNotifyMapper.insert(notify);
        return notify;
    }

    private void markNotifyProcessed(Long id, String message) {
        WechatPayNotify notify = wechatPayNotifyMapper.selectById(id);
        if (notify == null) {
            return;
        }
        notify.setProcessedFlag("OK".equals(message) ? 1 : 0);
        notify.setProcessMessage(message);
        wechatPayNotifyMapper.updateById(notify);
    }

    private String generateOutTradeNo() {
        return "BC" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String generateOutRefundNo() {
        return "WRF" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String buildPaymentDescription(BeerEntry entry) {
        return "啤酒大赛报名费-" + entry.getUuid();
    }

    private String buildStatusLogSummary(String action, String reason) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "action", action,
                    "reason", StringUtils.hasText(reason) ? reason : ""
            ));
        } catch (JsonProcessingException ex) {
            return action;
        }
    }

    private void writeEntryLog(Long adminId, String action, String targetPublicId, String summary) {
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

    private record RefundContext(EntryRefund refund, EntryPayment payment, BeerEntry entry) {
    }
}
