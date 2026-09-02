package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.PaymentOrderItemMapper;
import com.beercompetition.mapper.PaymentOrderMapper;
import com.beercompetition.mapper.RegistrationBatchMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.mapper.WechatPayNotifyMapper;
import com.beercompetition.billing.beercoin.BeerCoinService;
import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.properties.WechatPayProperties;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.PaymentOrderStatus;
import com.beercompetition.pojo.enums.RegistrationBatchStatus;
import com.beercompetition.pojo.enums.OrganizerType;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.PaymentOrder;
import com.beercompetition.pojo.po.PaymentOrderItem;
import com.beercompetition.pojo.po.RegistrationBatch;
import com.beercompetition.pojo.po.WechatPayNotify;
import com.beercompetition.pojo.vo.EntryPaymentStatusVO;
import com.beercompetition.pojo.vo.WechatJsapiPayVO;
import com.beercompetition.pojo.vo.WechatNativePayVO;
import com.beercompetition.pojo.vo.WechatPayClientConfigVO;
import com.beercompetition.service.WechatPaymentService;
import com.beercompetition.service.BatchPaymentService;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.WechatOAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private static final String BUSINESS_BEER_COIN_PURCHASE = "BEER_COIN_PURCHASE";
    private static final String BUSINESS_REFUND = "REFUND";
    private static final int NATIVE_PAY_EXPIRE_MINUTES = 30;
    private static final int JSAPI_PAY_EXPIRE_MINUTES = 30;
    private static final int REFUND_RECONCILE_DELAY_MINUTES = 2;
    private static final int REFUND_RECONCILE_BATCH_SIZE = 100;
    private final WechatPayClient wechatPayClient;
    private final BatchPaymentService batchPaymentService;
    private final WechatOAuthService wechatOAuthService;
    private final WechatPayProperties wechatPayProperties;
    private final PortalAccountMapper portalAccountMapper;
    private final BreweryMapper breweryMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final CompetitionMapper competitionMapper;
    private final OrganizerMapper organizerMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final EntryRefundMapper entryRefundMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentOrderItemMapper paymentOrderItemMapper;
    private final RegistrationBatchMapper registrationBatchMapper;
    private final WechatPayNotifyMapper wechatPayNotifyMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final CompetitionAccessService competitionAccessService;
    private final EntryScanLabelService entryScanLabelService;
    private final BeerCoinService beerCoinService;

    @Override
    public WechatNativePayVO createNativePayment(Long entryId) {
        // 1) 校验厂牌作品与支付资格
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        rejectTenantWechatPayment(entry.getCompetitionId());
        EntryPayment payment = ensurePayment(entry);
        assertStandalonePayment(payment);
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
    public WechatJsapiPayVO createJsapiPayment(Long entryId, String code) {
        // 1) 校验厂牌作品、支付资格与微信授权码
        if (!StringUtils.hasText(code)) {
            throw new BaseException("请先完成微信授权");
        }
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        rejectTenantWechatPayment(entry.getCompetitionId());
        EntryPayment payment = ensurePayment(entry);
        assertStandalonePayment(payment);
        if (EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            return toJsapiPayVO(payment, null);
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

        // 2) 获取 openid 并清理旧的未支付微信订单
        String openid = wechatOAuthService.resolveOpenid(code);
        closeUnpaidWechatOrder(payment, entry);
        payment = entryPaymentMapper.selectById(payment.getId());
        if (EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            return toJsapiPayVO(payment, null);
        }

        // 3) 调用微信 JSAPI 下单并保存订单
        String outTradeNo = generateOutTradeNo();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(JSAPI_PAY_EXPIRE_MINUTES);
        WechatPayClient.JsapiPayResult result = wechatPayClient.createJsapiPayment(
                new WechatPayClient.JsapiPayRequest(outTradeNo, buildPaymentDescription(entry),
                        payment.getAmount(), expireTime, openid));

        payment.setPayMethod(EntryPayMethod.WECHAT.name());
        payment.setStatus(EntryPaymentStatus.UNPAID.name());
        payment.setOutTradeNo(outTradeNo);
        payment.setCodeUrl(null);
        payment.setExpireTime(expireTime);
        payment.setWechatTradeState("NOTPAY");
        payment.setWechatTradeStateDesc("待支付");
        entryPaymentMapper.updateById(payment);
        return toJsapiPayVO(payment, result);
    }

    @Override
    public WechatPayClientConfigVO getClientConfig() {
        String appId = trimToNull(wechatPayProperties.getAppId());
        boolean jsapiConfigured = !wechatPayProperties.isWechatMode()
                || (appId != null && StringUtils.hasText(wechatPayProperties.getAppSecret()));
        return WechatPayClientConfigVO.builder()
                .mode(wechatPayProperties.normalizedMode())
                .appId(appId)
                .jsapiConfigured(jsapiConfigured)
                .build();
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
        WechatPayNotify existingNotify = findNotify(result.notifyId());
        if (existingNotify != null && Objects.equals(existingNotify.getProcessedFlag(), 1)) {
            return;
        }
        boolean beerCoinPurchase = beerCoinService.isPurchaseOrder(result.outTradeNo());
        WechatPayNotify notify = existingNotify;
        if (notify == null) {
            try {
                notify = insertNotify(result.notifyId(), result.eventType(),
                        beerCoinPurchase ? BUSINESS_BEER_COIN_PURCHASE : BUSINESS_PAYMENT,
                        result.outTradeNo(), null, result.transactionId(), null, result.rawJson());
            } catch (DuplicateKeyException ex) {
                notify = findNotify(result.notifyId());
                if (notify == null) {
                    throw ex;
                }
                if (Objects.equals(notify.getProcessedFlag(), 1)) {
                    return;
                }
            }
        }
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (beerCoinPurchase) {
                    beerCoinService.applyWechatPaymentSuccess(result);
                } else if (!batchPaymentService.applyWechatPaymentSuccess(result)) {
                    applyPaymentSuccess(result);
                }
            });
            markNotifyProcessed(notify.getId(), "OK");
        } catch (RuntimeException ex) {
            markNotifyProcessed(notify.getId(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public void handleRefundNotify(WechatPayClient.WechatNotifyRequest request) {
        WechatPayClient.RefundNotifyResult result = wechatPayClient.parseRefundNotify(request);
        WechatPayNotify existingNotify = findNotify(result.notifyId());
        if (existingNotify != null && Objects.equals(existingNotify.getProcessedFlag(), 1)) {
            return;
        }
        WechatPayNotify notify = existingNotify;
        if (notify == null) {
            try {
                notify = insertNotify(result.notifyId(), result.eventType(), BUSINESS_REFUND,
                        result.outTradeNo(), result.outRefundNo(), null, result.refundId(), result.rawJson());
            } catch (DuplicateKeyException ex) {
                notify = findNotify(result.notifyId());
                if (notify == null) {
                    throw ex;
                }
                if (Objects.equals(notify.getProcessedFlag(), 1)) {
                    return;
                }
            }
        }
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
        // 微信收款码由租户线下退款，租户确认时直接完成退款状态转换。
        requireAdminRefund(refundId);
        RefundContext context = transactionTemplate.execute(status -> {
            RefundContext prepared = prepareRefund(refundId, reason, adminId);
            if (isWechatQrPayment(prepared.payment())) {
                applyManualRefundSuccess(refundId, reason, adminId);
            }
            return prepared;
        });
        if (isWechatQrPayment(context.payment())) {
            return;
        }
        if (isManualRefundPayment(context.payment())) {
            return;
        }

        submitWechatRefund(context, true);
    }

    @Override
    public void autoApproveRefund(Long refundId, String reason) {
        // 1) 自动受理报名截止前的退款申请
        RefundContext context = transactionTemplate.execute(status -> prepareAutoRefund(refundId, reason));
        if (context == null) {
            return;
        }
        if (isManualRefundPayment(context.payment())) {
            return;
        }

        submitWechatRefund(context, false);
    }

    @Override
    public void retryRefund(Long refundId, String reason, Long adminId) {
        EntryRefund refund = requireAdminRefund(refundId);
        if (!EntryRefundStatus.FAILED.name().equals(refund.getStatus())) {
            throw new BaseException("只有退款失败记录可以重试");
        }
        EntryPayment payment = entryPaymentMapper.selectById(refund.getEntryPaymentId());
        if (isManualRefundPayment(payment)) {
            throw new BaseException("银行卡退款请上传转账凭证完成处理，不能重试微信退款");
        }
        if (StringUtils.hasText(refund.getOutRefundNo())) {
            try {
                WechatPayClient.RefundResult existing = wechatPayClient.queryRefund(refund.getOutRefundNo());
                if (existing != null) {
                    applyWechatRefundResult(existing);
                    return;
                }
            } catch (RuntimeException ex) {
                if (!isRefundNotFound(ex)) {
                    String failureReason = "微信退款状态暂时无法查询，请稍后重试";
                    transactionTemplate.executeWithoutResult(status -> markRefundFailed(refundId, failureReason));
                    throw new BaseException(failureReason);
                }
            }
        }
        approveRefund(refundId, reason, adminId);
    }

    @Override
    public int reconcileProcessingRefunds() {
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(REFUND_RECONCILE_DELAY_MINUTES);
        List<EntryRefund> refunds = entryRefundMapper.selectList(new LambdaQueryWrapper<EntryRefund>()
                .eq(EntryRefund::getStatus, EntryRefundStatus.PROCESSING.name())
                .isNotNull(EntryRefund::getOutRefundNo)
                .and(wrapper -> wrapper.isNull(EntryRefund::getProcessedTime)
                        .or().le(EntryRefund::getProcessedTime, staleBefore))
                .orderByAsc(EntryRefund::getId)
                .last("LIMIT " + REFUND_RECONCILE_BATCH_SIZE));
        int reconciledCount = 0;
        for (EntryRefund refund : refunds) {
            EntryPayment payment = entryPaymentMapper.selectById(refund.getEntryPaymentId());
            if (isManualRefundPayment(payment)) {
                continue;
            }
            try {
                WechatPayClient.RefundResult result = wechatPayClient.queryRefund(refund.getOutRefundNo());
                if (result == null) {
                    transactionTemplate.executeWithoutResult(status -> markRefundFailed(
                            refund.getId(), "微信未返回退款状态，请在后台重试"));
                } else {
                    applyWechatRefundResult(result);
                }
            } catch (RuntimeException ex) {
                String reason = isRefundNotFound(ex)
                        ? "微信未查询到退款单，请在后台重试"
                        : "微信退款状态查询失败，请稍后重试";
                transactionTemplate.executeWithoutResult(status -> markRefundFailed(refund.getId(), reason));
            }
            reconciledCount++;
        }
        return reconciledCount;
    }

    @Override
    public void completeOfflineRefund(Long refundId, String reason, Long adminId) {
        EntryRefund refund = requireAdminRefund(refundId);
        EntryPayment payment = entryPaymentMapper.selectById(refund.getEntryPaymentId());
        if (!isManualRefundPayment(payment)) {
            throw new BaseException("当前退款不支持人工确认");
        }
        boolean wechatQr = payment != null && EntryPayMethod.WECHAT_QR.name().equals(payment.getPayMethod());
        if (wechatQr) {
            if (!EntryRefundStatus.APPROVED.name().equals(refund.getStatus())) {
                throw new BaseException("只有已通过的微信收款码退款可以确认完成");
            }
        } else if (!EntryRefundStatus.PROCESSING.name().equals(refund.getStatus())) {
            throw new BaseException("只有已登记银行卡退款的记录可以确认完成");
        }
        transactionTemplate.executeWithoutResult(status -> applyManualRefundSuccess(refundId, reason, adminId));
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

    private void closeUnpaidWechatOrder(EntryPayment payment, BeerEntry entry) {
        if (!StringUtils.hasText(payment.getOutTradeNo()) || EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            return;
        }
        try {
            syncPaymentQuery(payment, entry);
            EntryPayment refreshed = entryPaymentMapper.selectById(payment.getId());
            if (refreshed != null && !EntryPaymentStatus.PAID.name().equals(refreshed.getStatus())
                    && StringUtils.hasText(refreshed.getOutTradeNo())) {
                wechatPayClient.closePayment(refreshed.getOutTradeNo());
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
        PaymentOrderItem orderItem = findOrderItem(payment);
        PaymentOrder order = orderItem == null ? null : paymentOrderMapper.selectById(orderItem.getPaymentOrderId());
        if (!isManualRefundPayment(payment)) {
            if (!StringUtils.hasText(order == null ? payment.getOutTradeNo() : order.getOutTradeNo())
                    && wechatPayProperties.isWechatMode()) {
                throw new BaseException("缺少支付订单号，无法发起微信退款");
            }
            if (order == null && !StringUtils.hasText(payment.getOutTradeNo())) {
                payment.setOutTradeNo(generateOutTradeNo());
                entryPaymentMapper.updateById(payment);
            }
            if (!StringUtils.hasText(refund.getOutRefundNo())) {
                refund.setOutRefundNo(generateOutRefundNo());
            }
        }
        refund.setStatus(isManualRefundPayment(payment)
                ? EntryRefundStatus.APPROVED.name()
                : EntryRefundStatus.PROCESSING.name());
        refund.setProcessedByAdminId(adminId);
        refund.setProcessedTime(LocalDateTime.now());
        refund.setFailReason(null);
        if (refund.getPaymentOrderItemId() == null && orderItem != null) {
            refund.setPaymentOrderItemId(orderItem.getId());
        }
        entryRefundMapper.updateById(refund);
        writeEntryLog(adminId, "ENTRY_REFUND_APPROVE", entry.getUuid(), buildStatusLogSummary("受理退款", reason));
        return new RefundContext(refund, payment, entry, order, orderItem);
    }

    private RefundContext prepareAutoRefund(Long refundId, String reason) {
        EntryRefund refund = requireRefund(refundId);
        if (!EntryRefundStatus.REQUESTED.name().equals(refund.getStatus())) {
            throw new BaseException("当前退款状态不能自动受理");
        }
        BeerEntry entry = requireEntry(refund.getBeerEntryId());
        EntryPayment payment = findPayment(entry.getId());
        if (payment == null || !EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            throw new BaseException("只有已支付报名可以退款");
        }
        PaymentOrderItem orderItem = findOrderItem(payment);
        PaymentOrder order = orderItem == null ? null : paymentOrderMapper.selectById(orderItem.getPaymentOrderId());
        if (!isManualRefundPayment(payment)
                && !StringUtils.hasText(order == null ? payment.getOutTradeNo() : order.getOutTradeNo())) {
            throw new BaseException("缺少支付订单号，无法发起微信退款");
        }
        if (!isManualRefundPayment(payment) && !StringUtils.hasText(refund.getOutRefundNo())) {
            refund.setOutRefundNo(generateOutRefundNo());
        }
        refund.setStatus(isManualRefundPayment(payment)
                ? EntryRefundStatus.APPROVED.name()
                : EntryRefundStatus.PROCESSING.name());
        refund.setProcessedByAdminId(null);
        refund.setProcessedTime(LocalDateTime.now());
        refund.setFailReason(null);
        if (refund.getPaymentOrderItemId() == null && orderItem != null) {
            refund.setPaymentOrderItemId(orderItem.getId());
        }
        entryRefundMapper.updateById(refund);
        return new RefundContext(refund, payment, entry, order, orderItem);
    }

    private void submitWechatRefund(RefundContext context, boolean propagateFailure) {
        try {
            WechatPayClient.RefundResult result = wechatPayClient.createRefund(new WechatPayClient.RefundRequest(
                    resolveRefundOutTradeNo(context),
                    resolveRefundTransactionId(context),
                    context.refund().getOutRefundNo(),
                    context.refund().getReason(),
                    context.refund().getAmount(),
                    resolveRefundTotalAmount(context)
            ));
            applyWechatRefundResult(result);
        } catch (RuntimeException ex) {
            if (!isDefinitiveRefundRejection(ex) && synchronizeRefundAfterUncertainFailure(context.refund())) {
                return;
            }
            String failureReason = resolveRefundFailureReason(ex);
            transactionTemplate.executeWithoutResult(status -> markRefundFailed(context.refund().getId(), failureReason));
            if (propagateFailure) {
                throw new BaseException(failureReason);
            }
        }
    }

    private boolean synchronizeRefundAfterUncertainFailure(EntryRefund refund) {
        try {
            WechatPayClient.RefundResult result = wechatPayClient.queryRefund(refund.getOutRefundNo());
            if (result == null) {
                return false;
            }
            applyWechatRefundResult(result);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void applyWechatRefundResult(WechatPayClient.RefundResult result) {
        transactionTemplate.executeWithoutResult(status -> applyRefundResult(result.outRefundNo(), result.refundId(),
                result.refundStatus(), result.successTime(), null));
    }

    private void markRefundFailed(Long refundId, String failureReason) {
        EntryRefund refund = requireRefund(refundId);
        if (EntryRefundStatus.SUCCESS.name().equals(refund.getStatus())) {
            return;
        }
        refund.setStatus(EntryRefundStatus.FAILED.name());
        refund.setFailReason(limitFailReason(failureReason));
        refund.setLastQueryTime(LocalDateTime.now());
        entryRefundMapper.updateById(refund);
    }

    private String resolveRefundFailureReason(RuntimeException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toUpperCase(Locale.ROOT);
        if (message.contains("NOT_ENOUGH")) {
            return "微信退款发起失败：商户基本账户余额不足";
        }
        if (message.contains("PARAM_ERROR") || message.contains("INVALID_REQUEST")) {
            return "微信退款发起失败：退款参数不符合微信要求";
        }
        if (message.contains("NO_AUTH")) {
            return "微信退款发起失败：商户号暂无退款权限";
        }
        if (message.contains("FREQUENCY_LIMITED")) {
            return "微信退款请求过于频繁，请稍后重试";
        }
        return "微信退款发起失败，请稍后重试";
    }

    private boolean isDefinitiveRefundRejection(RuntimeException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toUpperCase(Locale.ROOT);
        return message.contains("NOT_ENOUGH")
                || message.contains("PARAM_ERROR")
                || message.contains("INVALID_REQUEST")
                || message.contains("NO_AUTH")
                || message.contains("FREQUENCY_LIMITED");
    }

    private boolean isRefundNotFound(RuntimeException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toUpperCase(Locale.ROOT);
        return message.contains("RESOURCE_NOT_EXISTS")
                || message.contains("REFUND_NOT_EXIST")
                || message.contains("NOT_FOUND")
                || message.contains("HTTPSTATUSCODE[404]");
    }

    private void applyManualRefundSuccess(Long refundId, String reason, Long adminId) {
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
        applyAggregateRefundSuccess(refund, payment);

        entry.setStatus(EntryStatus.CANCELED.name());
        beerEntryMapper.updateById(entry);
        entryScanLabelService.disableActiveLabel(entry.getId());
        String action = isWechatQrPayment(payment) ? "微信退款完成" : "银行卡退款完成";
        writeEntryLog(adminId, "ENTRY_REFUND_SUCCESS", entry.getUuid(), buildStatusLogSummary(action, reason));
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
            applyAggregateRefundSuccess(refund, payment);

            entry.setStatus(EntryStatus.CANCELED.name());
            beerEntryMapper.updateById(entry);
            entryScanLabelService.disableActiveLabel(entry.getId());
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
                || EntryPayMethod.MANUAL.name().equals(payment.getPayMethod())
                || EntryPayMethod.WECHAT_QR.name().equals(payment.getPayMethod()));
    }

    private boolean isWechatQrPayment(EntryPayment payment) {
        return payment != null && EntryPayMethod.WECHAT_QR.name().equals(payment.getPayMethod());
    }

    private PaymentOrderItem findOrderItem(EntryPayment payment) {
        if (payment == null || payment.getPaymentOrderId() == null) {
            return null;
        }
        return paymentOrderItemMapper.selectOne(new LambdaQueryWrapper<PaymentOrderItem>()
                .eq(PaymentOrderItem::getEntryPaymentId, payment.getId())
                .last("LIMIT 1"));
    }

    private String resolveRefundOutTradeNo(RefundContext context) {
        return context.order() == null ? context.payment().getOutTradeNo() : context.order().getOutTradeNo();
    }

    private String resolveRefundTransactionId(RefundContext context) {
        return context.order() == null
                ? context.payment().getWechatTransactionId()
                : context.order().getWechatTransactionId();
    }

    private BigDecimal resolveRefundTotalAmount(RefundContext context) {
        return context.order() == null ? context.payment().getAmount() : context.order().getAmount();
    }

    private void applyAggregateRefundSuccess(EntryRefund refund, EntryPayment payment) {
        PaymentOrderItem item = refund.getPaymentOrderItemId() == null
                ? findOrderItem(payment)
                : paymentOrderItemMapper.selectById(refund.getPaymentOrderItemId());
        if (item == null) {
            return;
        }
        BigDecimal previousRefunded = item.getRefundedAmount() == null ? BigDecimal.ZERO : item.getRefundedAmount();
        BigDecimal refundAmount = refund.getAmount() == null ? BigDecimal.ZERO : refund.getAmount();
        BigDecimal itemRefunded = previousRefunded.add(refundAmount).min(item.getAmount());
        item.setRefundedAmount(itemRefunded);
        item.setStatus(itemRefunded.compareTo(item.getAmount()) >= 0
                ? EntryPaymentStatus.REFUNDED.name() : EntryPaymentStatus.PAID.name());
        paymentOrderItemMapper.updateById(item);

        PaymentOrder order = paymentOrderMapper.selectById(item.getPaymentOrderId());
        if (order == null) {
            return;
        }
        BigDecimal refundedAmount = paymentOrderItemMapper.selectList(new LambdaQueryWrapper<PaymentOrderItem>()
                        .eq(PaymentOrderItem::getPaymentOrderId, order.getId()))
                .stream()
                .map(PaymentOrderItem::getRefundedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean fullyRefunded = refundedAmount.compareTo(order.getAmount()) >= 0;
        order.setRefundedAmount(refundedAmount);
        order.setStatus(fullyRefunded
                ? PaymentOrderStatus.REFUNDED.name()
                : PaymentOrderStatus.PARTIALLY_REFUNDED.name());
        paymentOrderMapper.updateById(order);

        RegistrationBatch batch = registrationBatchMapper.selectById(order.getRegistrationBatchId());
        if (batch != null) {
            batch.setStatus(fullyRefunded
                    ? RegistrationBatchStatus.REFUNDED.name()
                    : RegistrationBatchStatus.PARTIALLY_REFUNDED.name());
            registrationBatchMapper.updateById(batch);
        }
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

    private WechatJsapiPayVO toJsapiPayVO(EntryPayment payment, WechatPayClient.JsapiPayResult result) {
        WechatJsapiPayVO.JsapiPayParams params = result == null ? null : WechatJsapiPayVO.JsapiPayParams.builder()
                .appId(result.appId())
                .timeStamp(result.timeStamp())
                .nonceStr(result.nonceStr())
                .packageValue(result.packageValue())
                .signType(result.signType())
                .paySign(result.paySign())
                .build();
        return WechatJsapiPayVO.builder()
                .mode(wechatPayProperties.normalizedMode())
                .outTradeNo(payment.getOutTradeNo())
                .amount(payment.getAmount())
                .expireTime(payment.getExpireTime())
                .paymentStatus(payment.getStatus())
                .payParams(params)
                .build();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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

    private void assertStandalonePayment(EntryPayment payment) {
        if (payment != null && payment.getPaymentOrderId() != null) {
            throw new BaseException("该酒款已加入统一付款订单，请返回报名订单完成支付");
        }
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

    private EntryRefund requireAdminRefund(Long refundId) {
        EntryRefund refund = requireRefund(refundId);
        BeerEntry entry = requireEntry(refund.getBeerEntryId());
        competitionAccessService.requireCompetitionAccess(entry.getCompetitionId());
        return refund;
    }

    private void rejectTenantWechatPayment(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        Organizer organizer = competition == null ? null : organizerMapper.selectById(competition.getOrganizerId());
        if (organizer != null && OrganizerType.TENANT.name().equals(organizer.getOrganizerType())) {
            throw new BaseException("该赛事请使用主办方收款码或银行转账");
        }
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

    private WechatPayNotify findNotify(String notifyId) {
        if (!StringUtils.hasText(notifyId)) {
            return null;
        }
        return wechatPayNotifyMapper.selectOne(new LambdaQueryWrapper<WechatPayNotify>()
                .eq(WechatPayNotify::getNotifyId, notifyId)
                .last("LIMIT 1"));
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

    private record RefundContext(EntryRefund refund, EntryPayment payment, BeerEntry entry,
                                 PaymentOrder order, PaymentOrderItem orderItem) {
    }
}
