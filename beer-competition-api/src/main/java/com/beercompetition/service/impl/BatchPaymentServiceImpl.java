package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.PaymentOrderItemMapper;
import com.beercompetition.mapper.PaymentOrderMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.RegistrationBatchMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.properties.WechatPayProperties;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.PaymentOrderStatus;
import com.beercompetition.pojo.enums.RegistrationBatchStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.PaymentOrder;
import com.beercompetition.pojo.po.PaymentOrderItem;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.RegistrationBatch;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.pojo.enums.OrganizerType;
import com.beercompetition.pojo.vo.PaymentOrderStatusVO;
import com.beercompetition.pojo.vo.WechatJsapiPayVO;
import com.beercompetition.pojo.vo.WechatNativePayVO;
import com.beercompetition.service.BatchPaymentService;
import com.beercompetition.service.WechatOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchPaymentServiceImpl implements BatchPaymentService {

    private static final int NATIVE_PAY_EXPIRE_MINUTES = 30;

    private static final int EXPIRY_SWEEP_LIMIT = 200;

    private static final Set<String> WECHAT_TERMINAL_FAILURE_STATES = Set.of("CLOSED", "REVOKED", "PAYERROR");

    private final WechatPayClient wechatPayClient;
    private final WechatPayProperties wechatPayProperties;
    private final WechatOAuthService wechatOAuthService;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentOrderItemMapper paymentOrderItemMapper;
    private final RegistrationBatchMapper registrationBatchMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final CompetitionMapper competitionMapper;
    private final OrganizerMapper organizerMapper;
    private final CompetitionAccessService competitionAccessService;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatNativePayVO createNativePayment(Long orderId) {
        // 1) 校验订单归属和支付资格
        PaymentOrder order = requireOwnedOrderForUpdate(orderId);
        rejectTenantWechatPayment(order);
        order = reconcileOrderFromItemPayments(order);
        order = expireOrderIfNeeded(order);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            return toNativePayVO(order);
        }
        if (PaymentOrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())) {
            throw new BaseException("银行转账信息已提交，请等待组委会核对到账");
        }
        order = reopenExpiredOrderForPayment(order);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            return toNativePayVO(order);
        }
        if (!PaymentOrderStatus.UNPAID.name().equals(order.getStatus())) {
            throw new BaseException("当前订单不能继续支付");
        }

        // 2) 复用仍有效的二维码，过期订单先查单再关闭
        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.hasText(order.getCodeUrl()) && order.getExpireTime() != null
                && order.getExpireTime().isAfter(now.plusSeconds(30))) {
            return toNativePayVO(order);
        }
        if (StringUtils.hasText(order.getOutTradeNo())) {
            order = synchronizeWechatPayment(order, true);
            if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
                return toNativePayVO(order);
            }
            closeExistingWechatOrder(order);
        }

        // 3) 创建微信订单并保存支付信息
        String outTradeNo = generateOutTradeNo();
        LocalDateTime expireTime = now.plusMinutes(NATIVE_PAY_EXPIRE_MINUTES);
        RegistrationBatch batch = requireBatch(order.getRegistrationBatchId());
        WechatPayClient.NativePayResult result = wechatPayClient.createNativePayment(
                new WechatPayClient.NativePayRequest(outTradeNo,
                        "啤酒大赛报名费-" + batch.getBatchNo(), order.getAmount(), expireTime));
        order.setPayMethod(EntryPayMethod.WECHAT.name());
        order.setOutTradeNo(outTradeNo);
        order.setCodeUrl(result.codeUrl());
        order.setExpireTime(expireTime);
        order.setWechatTradeState("NOTPAY");
        order.setWechatTradeStateDesc("待支付");
        order.setLastQueryTime(null);
        paymentOrderMapper.updateById(order);
        return toNativePayVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatJsapiPayVO createJsapiPayment(Long orderId, String code) {
        // 1) 校验订单归属、支付资格和微信授权
        PaymentOrder order = requireOwnedOrderForUpdate(orderId);
        rejectTenantWechatPayment(order);
        order = reconcileOrderFromItemPayments(order);
        order = expireOrderIfNeeded(order);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            return toJsapiPayVO(order, null);
        }
        if (PaymentOrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())) {
            throw new BaseException("银行转账信息已提交，请等待组委会核对到账");
        }
        order = reopenExpiredOrderForPayment(order);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            return toJsapiPayVO(order, null);
        }
        if (!PaymentOrderStatus.UNPAID.name().equals(order.getStatus())) {
            throw new BaseException("当前订单不能继续支付");
        }
        String openid = wechatOAuthService.resolveOpenid(code);

        // 2) 查询并关闭旧微信订单，避免 Native 与 JSAPI 同时有效
        order = synchronizeWechatPayment(order, true);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            return toJsapiPayVO(order, null);
        }
        closeExistingWechatOrder(order);

        // 3) 创建整批 JSAPI 订单并保存支付标识
        String outTradeNo = generateOutTradeNo();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(NATIVE_PAY_EXPIRE_MINUTES);
        RegistrationBatch batch = requireBatch(order.getRegistrationBatchId());
        WechatPayClient.JsapiPayResult result = wechatPayClient.createJsapiPayment(
                new WechatPayClient.JsapiPayRequest(outTradeNo,
                        "啤酒大赛报名费-" + batch.getBatchNo(), order.getAmount(), expireTime, openid));
        order.setPayMethod(EntryPayMethod.WECHAT.name());
        order.setOutTradeNo(outTradeNo);
        order.setCodeUrl(null);
        order.setExpireTime(expireTime);
        order.setWechatTradeState("NOTPAY");
        order.setWechatTradeStateDesc("待支付");
        order.setLastQueryTime(null);
        paymentOrderMapper.updateById(order);
        return toJsapiPayVO(order, result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderStatusVO getPortalPaymentStatus(Long orderId) {
        // 1) 校验并读取当前厂商订单
        PaymentOrder order = requireOwnedOrderForUpdate(orderId);
        order = reconcileOrderFromItemPayments(order);

        // 2) 微信回调延迟时主动查单补偿
        order = synchronizeWechatPayment(order, false);

        // 3) 超时未支付落过期状态，避免长期停留在待支付
        order = expireOrderIfNeeded(order);

        // 4) 返回聚合订单状态
        return toStatusVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderStatusVO simulatePayment(Long orderId) {
        // 1) 限制模拟支付只在非真实微信模式使用
        if (wechatPayProperties.isWechatMode()) {
            throw new BaseException("当前已启用微信支付，请扫码完成报名费支付");
        }
        PaymentOrder order = requireOwnedOrderForUpdate(orderId);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            return toStatusVO(order);
        }
        order = reopenExpiredOrderForPayment(order);
        if (!PaymentOrderStatus.UNPAID.name().equals(order.getStatus())) {
            throw new BaseException("当前订单不能模拟支付");
        }

        // 2) 推进订单及所有酒款付款状态
        if (!StringUtils.hasText(order.getOutTradeNo())) {
            order.setOutTradeNo(generateOutTradeNo());
        }
        applyPaymentSuccess(order, EntryPayMethod.MOCK.name(), null, order.getAmount(), LocalDateTime.now(), null);

        // 3) 返回最新订单状态
        return toStatusVO(paymentOrderMapper.selectById(orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmManualPayment(Long orderId, Long adminId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BaseException("请填写人工确认收款的到账依据");
        }
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, orderId).last("LIMIT 1 FOR UPDATE"));
        if (order == null) {
            throw new ResourceNotFoundException("支付订单不存在");
        }
        RegistrationBatch batch = requireBatch(order.getRegistrationBatchId());
        competitionAccessService.requireCompetitionAccess(batch.getCompetitionId());
        if (!Set.of(PaymentOrderStatus.UNPAID.name(), PaymentOrderStatus.EXPIRED.name())
                .contains(order.getStatus())) {
            throw new BaseException("当前订单不能人工确认收款");
        }
        validatePayableItems(order);
        if (wechatPayProperties.isWechatMode() && StringUtils.hasText(order.getOutTradeNo())) {
            WechatPayClient.PaymentQueryResult result = wechatPayClient.queryPayment(order.getOutTradeNo());
            if ("SUCCESS".equals(result.tradeState())) {
                if (result.paidAmount() != null && order.getAmount().compareTo(result.paidAmount()) != 0) {
                    throw new BaseException("微信支付金额不一致，请先核对流水");
                }
                applyPaymentSuccess(order, EntryPayMethod.WECHAT.name(), result.transactionId(),
                        result.paidAmount() == null ? order.getAmount() : result.paidAmount(),
                        result.paidTime() == null ? LocalDateTime.now() : result.paidTime(), null);
                adminOperationLogMapper.insert(AdminOperationLog.builder()
                        .adminUserId(adminId).competitionId(batch.getCompetitionId())
                        .action("ENTRY_CONFIRM_PAYMENT").targetType("PAYMENT_ORDER")
                        .targetPublicId(order.getOrderNo())
                        .summary("管理端微信查单确认到账：" + reason.trim()).build());
                return;
            }
            if (!WECHAT_TERMINAL_FAILURE_STATES.contains(result.tradeState())) {
                wechatPayClient.closePayment(order.getOutTradeNo());
            }
            order.setCodeUrl(null);
            order.setWechatTradeState("CLOSED");
            order.setWechatTradeStateDesc("人工确认前已关闭微信支付");
        }
        LocalDateTime now = LocalDateTime.now();
        applyPaymentSuccess(order, EntryPayMethod.MANUAL.name(), null, order.getAmount(), now, null);
        order.setManualConfirmedByAdminId(adminId);
        order.setManualConfirmedTime(now);
        order.setManualConfirmRemark(reason.trim());
        paymentOrderMapper.updateById(order);
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(adminId).competitionId(batch.getCompetitionId())
                .action("ENTRY_CONFIRM_PAYMENT").targetType("PAYMENT_ORDER")
                .targetPublicId(order.getOrderNo())
                .summary("人工确认整批收款：" + reason.trim()).build());
        for (PaymentOrderItem item : listItems(order.getId())) {
            EntryPayment payment = entryPaymentMapper.selectById(item.getEntryPaymentId());
            payment.setConfirmedByAdminId(adminId);
            payment.setConfirmRemark(reason.trim());
            entryPaymentMapper.updateById(payment);
            BeerEntry entry = beerEntryMapper.selectById(item.getBeerEntryId());
            adminOperationLogMapper.insert(AdminOperationLog.builder()
                    .adminUserId(adminId).competitionId(batch.getCompetitionId())
                    .action("ENTRY_CONFIRM_PAYMENT").targetType("BEER_ENTRY")
                    .targetPublicId(entry.getUuid())
                    .summary("人工确认统一订单收款：" + order.getOrderNo() + "；" + reason.trim()).build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelUnpaidOrder(Long orderId, Long adminId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BaseException("请填写整批取消原因");
        }
        PaymentOrder order = requireOrderForUpdate(orderId);
        RegistrationBatch batch = requireBatch(order.getRegistrationBatchId());
        competitionAccessService.requireCompetitionAccess(batch.getCompetitionId());
        if (!Set.of(PaymentOrderStatus.UNPAID.name(), PaymentOrderStatus.EXPIRED.name())
                .contains(order.getStatus())) {
            throw new BaseException("当前订单不能整批取消报名");
        }
        validatePayableItems(order);
        if (wechatPayProperties.isWechatMode() && StringUtils.hasText(order.getOutTradeNo())) {
            WechatPayClient.PaymentQueryResult result = wechatPayClient.queryPayment(order.getOutTradeNo());
            if ("SUCCESS".equals(result.tradeState())) {
                throw new BaseException("微信支付已到账，请先核对收款");
            }
            if (!WECHAT_TERMINAL_FAILURE_STATES.contains(result.tradeState())) {
                wechatPayClient.closePayment(order.getOutTradeNo());
            }
        }
        order.setStatus(PaymentOrderStatus.CANCELED.name());
        order.setCodeUrl(null);
        paymentOrderMapper.updateById(order);
        batch.setStatus(RegistrationBatchStatus.CANCELED.name());
        registrationBatchMapper.updateById(batch);
        for (PaymentOrderItem item : listItems(orderId)) {
            EntryPayment payment = entryPaymentMapper.selectById(item.getEntryPaymentId());
            BeerEntry entry = beerEntryMapper.selectById(item.getBeerEntryId());
            payment.setStatus(EntryPaymentStatus.CANCELED.name());
            payment.setConfirmRemark(reason.trim());
            entryPaymentMapper.updateById(payment);
            item.setStatus(EntryPaymentStatus.CANCELED.name());
            paymentOrderItemMapper.updateById(item);
            entry.setStatus(EntryStatus.CANCELED.name());
            beerEntryMapper.updateById(entry);
            adminOperationLogMapper.insert(AdminOperationLog.builder()
                    .adminUserId(adminId).competitionId(batch.getCompetitionId())
                    .action("ENTRY_CANCEL").targetType("BEER_ENTRY")
                    .targetPublicId(entry.getUuid())
                    .summary("人工取消统一订单报名：" + order.getOrderNo() + "；" + reason.trim()).build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyWechatPaymentSuccess(WechatPayClient.PaymentNotifyResult result) {
        // 1) 按微信商户单号识别聚合订单
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOutTradeNo, result.outTradeNo())
                .last("LIMIT 1 FOR UPDATE"));
        if (order == null) {
            return false;
        }
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            if (!EntryPayMethod.WECHAT.name().equals(order.getPayMethod())) {
                throw new BaseException("该微信订单与已确认的线下收款冲突，请核对重复收款");
            }
            return true;
        }

        // 2) 校验微信状态和实付金额
        if (!"SUCCESS".equals(result.tradeState())) {
            throw new BaseException("微信支付未成功");
        }
        if (result.paidAmount() != null && order.getAmount().compareTo(result.paidAmount()) != 0) {
            throw new BaseException("微信支付金额不一致");
        }

        // 3) 推进聚合订单和所有酒款状态
        applyPaymentSuccess(order, EntryPayMethod.WECHAT.name(), result.transactionId(),
                result.paidAmount() == null ? order.getAmount() : result.paidAmount(),
                result.paidTime() == null ? LocalDateTime.now() : result.paidTime(), result.rawJson());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markBankTransferPending(Long orderId, Long transferId) {
        // 1) 校验当前厂商订单和待支付状态
        PaymentOrder order = requireOwnedOrderForUpdate(orderId);
        order = reconcileOrderFromItemPayments(order);
        order = synchronizeWechatPayment(order, true);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            throw new BaseException("微信支付已到账，无需再提交银行转账");
        }
        order = reopenExpiredOrderForPayment(order);
        if (!PaymentOrderStatus.UNPAID.name().equals(order.getStatus())) {
            throw new BaseException("当前订单不能提交银行转账");
        }

        // 2) 关闭仍未支付的微信订单
        closeExistingWechatOrder(order);

        // 3) 锁定订单与逐款付款记录
        order.setStatus(PaymentOrderStatus.PENDING_CONFIRM.name());
        order.setPayMethod(EntryPayMethod.BANK_TRANSFER.name());
        order.setBankTransferId(transferId);
        paymentOrderMapper.updateById(order);
        updateItemPayments(orderId, EntryPaymentStatus.PENDING_CONFIRM.name(), EntryPayMethod.BANK_TRANSFER.name(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markOrganizerPaymentPending(Long orderId, Long transferId) {
        PaymentOrder order = requireOrderForUpdate(orderId);
        order = synchronizeWechatPayment(order, true);
        order = reopenExpiredOrderForPayment(order);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            throw new BaseException("微信支付已到账，无需再提交付款确认");
        }
        if (!PaymentOrderStatus.UNPAID.name().equals(order.getStatus())) {
            throw new BaseException("当前订单不能提交付款确认");
        }
        closeExistingWechatOrder(order);
        order.setStatus(PaymentOrderStatus.PENDING_CONFIRM.name());
        order.setPayMethod(EntryPayMethod.WECHAT_QR.name());
        order.setBankTransferId(transferId);
        order.setManualSubmitTime(LocalDateTime.now());
        paymentOrderMapper.updateById(order);
        updateItemPayments(orderId, EntryPaymentStatus.PENDING_CONFIRM.name(), EntryPayMethod.WECHAT_QR.name(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBankTransfer(Long orderId, Long transferId, Long adminId) {
        // 1) 校验转账记录与聚合订单关联
        PaymentOrder order = requireOrder(orderId);
        if (!PaymentOrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())
                || !Objects.equals(order.getBankTransferId(), transferId)) {
            throw new BaseException("银行转账关联订单状态异常");
        }

        // 2) 推进订单和所有酒款为已支付
        applyPaymentSuccess(order, EntryPayMethod.BANK_TRANSFER.name(), null,
                order.getAmount(), LocalDateTime.now(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrganizerPayment(Long orderId, Long transferId, Long adminId) {
        PaymentOrder order = requireOrder(orderId);
        if (!PaymentOrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())
                || !Objects.equals(order.getBankTransferId(), transferId)) {
            throw new BaseException("收款确认关联订单状态异常");
        }
        applyPaymentSuccess(order, EntryPayMethod.WECHAT_QR.name(), null,
                order.getAmount(), LocalDateTime.now(), null);
        order.setManualConfirmedByAdminId(adminId);
        order.setManualConfirmedTime(LocalDateTime.now());
        paymentOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetBankTransfer(Long orderId, Long transferId) {
        // 1) 校验待确认订单与转账记录
        PaymentOrder order = requireOrder(orderId);
        if (!PaymentOrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())
                || !Objects.equals(order.getBankTransferId(), transferId)) {
            return;
        }

        // 2) 释放聚合订单和逐款付款记录
        order.setStatus(PaymentOrderStatus.UNPAID.name());
        order.setPayMethod(EntryPayMethod.MANUAL.name());
        order.setBankTransferId(null);
        paymentOrderMapper.updateById(order);
        updateItemPayments(orderId, EntryPaymentStatus.UNPAID.name(), EntryPayMethod.MANUAL.name(), null);
    }

    private void applyPaymentSuccess(PaymentOrder order, String payMethod, String transactionId,
                                     BigDecimal paidAmount, LocalDateTime paidTime, String rawJson) {
        order.setStatus(PaymentOrderStatus.PAID.name());
        order.setPayMethod(payMethod);
        order.setWechatTransactionId(transactionId);
        order.setPaidAmount(paidAmount);
        order.setPaidTime(paidTime);
        if (rawJson != null) {
            order.setNotifyRawJson(rawJson);
        }
        if (EntryPayMethod.WECHAT.name().equals(payMethod)) {
            order.setWechatTradeState("SUCCESS");
            order.setWechatTradeStateDesc("支付成功");
        }
        paymentOrderMapper.updateById(order);

        updateItemPayments(order.getId(), EntryPaymentStatus.PAID.name(), payMethod, paidTime);
        RegistrationBatch batch = requireBatch(order.getRegistrationBatchId());
        batch.setStatus(RegistrationBatchStatus.PAID.name());
        registrationBatchMapper.updateById(batch);
    }

    private void updateItemPayments(Long orderId, String paymentStatus, String payMethod, LocalDateTime paidTime) {
        List<PaymentOrderItem> items = listItems(orderId);
        if (items.isEmpty()) {
            throw new BaseException("支付订单没有关联酒款");
        }
        for (PaymentOrderItem item : items) {
            EntryPayment payment = entryPaymentMapper.selectById(item.getEntryPaymentId());
            BeerEntry entry = beerEntryMapper.selectById(item.getBeerEntryId());
            if (payment == null || entry == null) {
                throw new BaseException("聚合订单酒款数据异常");
            }
            if (EntryPaymentStatus.PAID.name().equals(paymentStatus)
                    && (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())
                    || !Set.of(EntryPaymentStatus.UNPAID.name(), EntryPaymentStatus.EXPIRED.name(),
                    EntryPaymentStatus.PENDING_CONFIRM.name()).contains(payment.getStatus()))) {
                throw new BaseException("订单中存在不处于待支付状态的酒款，请先核对报名");
            }
            payment.setStatus(paymentStatus);
            payment.setPayMethod(payMethod);
            if (EntryPaymentStatus.PAID.name().equals(paymentStatus)) {
                payment.setPaidAmount(payment.getAmount());
                payment.setPaidTime(paidTime);
                entry.setStatus(EntryStatus.REGISTERED.name());
                beerEntryMapper.updateById(entry);
            }
            entryPaymentMapper.updateById(payment);
            item.setStatus(paymentStatus);
            paymentOrderItemMapper.updateById(item);
        }
    }

    private void validatePayableItems(PaymentOrder order) {
        List<PaymentOrderItem> items = listItems(order.getId());
        if (items.isEmpty()) {
            throw new BaseException("支付订单没有关联酒款");
        }
        BigDecimal amount = BigDecimal.ZERO;
        for (PaymentOrderItem item : items) {
            EntryPayment payment = entryPaymentMapper.selectById(item.getEntryPaymentId());
            BeerEntry entry = beerEntryMapper.selectById(item.getBeerEntryId());
            if (payment == null || entry == null
                    || !Objects.equals(payment.getPaymentOrderId(), order.getId())
                    || !Objects.equals(payment.getBeerEntryId(), item.getBeerEntryId())
                    || item.getAmount() == null || payment.getAmount() == null
                    || item.getAmount().compareTo(payment.getAmount()) != 0
                    || !EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())
                    || !Set.of(EntryPaymentStatus.UNPAID.name(), EntryPaymentStatus.EXPIRED.name()).contains(payment.getStatus())) {
                throw new BaseException("订单中存在不处于待支付状态的酒款，请先核对报名");
            }
            amount = amount.add(item.getAmount());
        }
        if (amount.compareTo(order.getAmount()) != 0) {
            throw new BaseException("订单与酒款金额不一致，请先核对账单");
        }
    }

    private PaymentOrder requireOwnedOrder(Long orderId) {
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("厂牌账号不存在");
        }
        PaymentOrder order = requireOrder(orderId);
        RegistrationBatch batch = requireBatch(order.getRegistrationBatchId());
        if (!Objects.equals(batch.getBreweryId(), account.getBreweryId())) {
            throw new ForbiddenException("无权操作该支付订单");
        }
        return order;
    }

    private PaymentOrder requireOwnedOrderForUpdate(Long orderId) {
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("厂牌账号不存在");
        }
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, orderId)
                .last("LIMIT 1 FOR UPDATE"));
        if (order == null) {
            throw new ResourceNotFoundException("支付订单不存在");
        }
        RegistrationBatch batch = requireBatch(order.getRegistrationBatchId());
        if (!Objects.equals(batch.getBreweryId(), account.getBreweryId())) {
            throw new ForbiddenException("无权操作该支付订单");
        }
        return order;
    }

    private PaymentOrder requireOrder(Long orderId) {
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("支付订单不存在");
        }
        return order;
    }

    private PaymentOrder requireOrderForUpdate(Long orderId) {
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, orderId).last("LIMIT 1 FOR UPDATE"));
        if (order == null) {
            throw new ResourceNotFoundException("支付订单不存在");
        }
        return order;
    }

    private void rejectTenantWechatPayment(PaymentOrder order) {
        RegistrationBatch batch = requireBatch(order.getRegistrationBatchId());
        Competition competition = competitionMapper.selectById(batch.getCompetitionId());
        Organizer organizer = competition == null ? null : organizerMapper.selectById(competition.getOrganizerId());
        if (organizer != null && OrganizerType.TENANT.name().equals(organizer.getOrganizerType())) {
            throw new BaseException("该赛事请使用主办方收款码或银行转账");
        }
    }

    private RegistrationBatch requireBatch(Long batchId) {
        RegistrationBatch batch = registrationBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new ResourceNotFoundException("报名批次不存在");
        }
        return batch;
    }

    private List<PaymentOrderItem> listItems(Long orderId) {
        return paymentOrderItemMapper.selectList(new LambdaQueryWrapper<PaymentOrderItem>()
                .eq(PaymentOrderItem::getPaymentOrderId, orderId)
                .orderByAsc(PaymentOrderItem::getId));
    }

    private WechatNativePayVO toNativePayVO(PaymentOrder order) {
        return WechatNativePayVO.builder()
                .mode(wechatPayProperties.normalizedMode())
                .outTradeNo(order.getOutTradeNo())
                .amount(order.getAmount())
                .codeUrl(order.getCodeUrl())
                .expireTime(order.getExpireTime())
                .paymentStatus(order.getStatus())
                .build();
    }

    private WechatJsapiPayVO toJsapiPayVO(PaymentOrder order, WechatPayClient.JsapiPayResult result) {
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
                .outTradeNo(order.getOutTradeNo())
                .amount(order.getAmount())
                .expireTime(order.getExpireTime())
                .paymentStatus(order.getStatus())
                .payParams(params)
                .build();
    }

    private PaymentOrder synchronizeWechatPayment(PaymentOrder order, boolean force) {
        if (!wechatPayProperties.isWechatMode()
                || !PaymentOrderStatus.UNPAID.name().equals(order.getStatus())
                || !StringUtils.hasText(order.getOutTradeNo())) {
            return order;
        }
        if (!force && order.getLastQueryTime() != null
                && order.getLastQueryTime().isAfter(LocalDateTime.now().minusSeconds(10))) {
            return order;
        }
        WechatPayClient.PaymentQueryResult result = wechatPayClient.queryPayment(order.getOutTradeNo());
        order.setWechatTransactionId(result.transactionId());
        order.setWechatTradeState(result.tradeState());
        order.setWechatTradeStateDesc(result.tradeStateDesc());
        order.setLastQueryTime(LocalDateTime.now());
        if ("SUCCESS".equals(result.tradeState())) {
            if (result.paidAmount() != null && order.getAmount().compareTo(result.paidAmount()) != 0) {
                throw new BaseException("微信支付金额不一致");
            }
            applyPaymentSuccess(order, EntryPayMethod.WECHAT.name(), result.transactionId(),
                    result.paidAmount() == null ? order.getAmount() : result.paidAmount(),
                    result.paidTime() == null ? LocalDateTime.now() : result.paidTime(), null);
            return paymentOrderMapper.selectById(order.getId());
        }
        if (WECHAT_TERMINAL_FAILURE_STATES.contains(result.tradeState())) {
            order.setStatus(PaymentOrderStatus.EXPIRED.name());
            order.setCodeUrl(null);
            updateItemPayments(order.getId(), EntryPaymentStatus.EXPIRED.name(), order.getPayMethod(), null);
        }
        paymentOrderMapper.updateById(order);
        return order;
    }

    private void closeExistingWechatOrder(PaymentOrder order) {
        if (!StringUtils.hasText(order.getOutTradeNo())) {
            return;
        }
        if (wechatPayProperties.isWechatMode()
                && !WECHAT_TERMINAL_FAILURE_STATES.contains(order.getWechatTradeState())) {
            wechatPayClient.closePayment(order.getOutTradeNo());
        }
        order.setOutTradeNo(null);
        order.setCodeUrl(null);
        order.setExpireTime(null);
        order.setWechatTradeState("CLOSED");
        order.setWechatTradeStateDesc("已更换付款方式");
        order.setLastQueryTime(LocalDateTime.now());
        paymentOrderMapper.updateById(order);
    }

    /**
     * 超时未支付时落过期状态。过期只关闭支付入口，不回退酒款状态，厂商仍可原地重新发起付款。
     * 先主动查单，避免微信回调延迟导致把已到账订单误判为过期。
     */
    private PaymentOrder expireOrderIfNeeded(PaymentOrder order) {
        if (order == null || !PaymentOrderStatus.UNPAID.name().equals(order.getStatus())
                || order.getExpireTime() == null
                || order.getExpireTime().isAfter(LocalDateTime.now())) {
            return order;
        }
        PaymentOrder synced = synchronizeWechatPayment(order, true);
        if (!PaymentOrderStatus.UNPAID.name().equals(synced.getStatus())) {
            return synced;
        }
        if (!closeWechatOrderOnExpire(synced)) {
            return paymentOrderMapper.selectById(synced.getId());
        }
        synced.setStatus(PaymentOrderStatus.EXPIRED.name());
        synced.setWechatTradeState("EXPIRED");
        synced.setWechatTradeStateDesc("支付已过期");
        paymentOrderMapper.updateById(synced);
        updateItemPayments(synced.getId(), EntryPaymentStatus.EXPIRED.name(), synced.getPayMethod(), null);
        return paymentOrderMapper.selectById(synced.getId());
    }

    private boolean closeWechatOrderOnExpire(PaymentOrder order) {
        if (!wechatPayProperties.isWechatMode() || !StringUtils.hasText(order.getOutTradeNo())) {
            return true;
        }
        try {
            wechatPayClient.closePayment(order.getOutTradeNo());
        } catch (Exception ex) {
            WechatPayClient.PaymentQueryResult result = wechatPayClient.queryPayment(order.getOutTradeNo());
            if ("SUCCESS".equals(result.tradeState())) {
                if (result.paidAmount() != null && order.getAmount().compareTo(result.paidAmount()) != 0) {
                    throw new BaseException("微信支付金额不一致，请先核对流水");
                }
                applyPaymentSuccess(order, EntryPayMethod.WECHAT.name(), result.transactionId(),
                        result.paidAmount() == null ? order.getAmount() : result.paidAmount(),
                        result.paidTime() == null ? LocalDateTime.now() : result.paidTime(), null);
                return false;
            }
            if (!WECHAT_TERMINAL_FAILURE_STATES.contains(result.tradeState())) {
                throw new BaseException("微信支付单关闭失败，请稍后重试");
            }
        }
        order.setCodeUrl(null);
        return true;
    }

    /**
     * 过期订单原地复用：回到待支付并清空旧微信支付标识，重新下单时不新建批次。
     */
    private PaymentOrder reopenExpiredOrderForPayment(PaymentOrder order) {
        if (!PaymentOrderStatus.EXPIRED.name().equals(order.getStatus())) {
            return order;
        }
        if (wechatPayProperties.isWechatMode() && StringUtils.hasText(order.getOutTradeNo())) {
            WechatPayClient.PaymentQueryResult result = wechatPayClient.queryPayment(order.getOutTradeNo());
            if ("SUCCESS".equals(result.tradeState())) {
                if (result.paidAmount() != null && order.getAmount().compareTo(result.paidAmount()) != 0) {
                    throw new BaseException("微信支付金额不一致，请先核对流水");
                }
                applyPaymentSuccess(order, EntryPayMethod.WECHAT.name(), result.transactionId(),
                        result.paidAmount() == null ? order.getAmount() : result.paidAmount(),
                        result.paidTime() == null ? LocalDateTime.now() : result.paidTime(), null);
                return paymentOrderMapper.selectById(order.getId());
            }
            if (!WECHAT_TERMINAL_FAILURE_STATES.contains(result.tradeState())) {
                wechatPayClient.closePayment(order.getOutTradeNo());
            }
        }
        order.setStatus(PaymentOrderStatus.UNPAID.name());
        order.setOutTradeNo(null);
        order.setCodeUrl(null);
        order.setExpireTime(null);
        order.setWechatTradeState(null);
        order.setWechatTradeStateDesc(null);
        order.setLastQueryTime(null);
        paymentOrderMapper.updateById(order);
        updateItemPayments(order.getId(), EntryPaymentStatus.UNPAID.name(), EntryPayMethod.MANUAL.name(), null);
        return paymentOrderMapper.selectById(order.getId());
    }

    @Override
    public int expireOverdueOrders() {
        List<PaymentOrder> overdue = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getStatus, PaymentOrderStatus.UNPAID.name())
                .isNotNull(PaymentOrder::getExpireTime)
                .lt(PaymentOrder::getExpireTime, LocalDateTime.now())
                .orderByAsc(PaymentOrder::getExpireTime)
                .last("LIMIT " + EXPIRY_SWEEP_LIMIT));
        int expired = 0;
        for (PaymentOrder order : overdue) {
            try {
                Boolean changed = transactionTemplate.execute(status -> {
                    PaymentOrder locked = requireOrderForUpdate(order.getId());
                    return PaymentOrderStatus.EXPIRED.name().equals(expireOrderIfNeeded(locked).getStatus());
                });
                if (Boolean.TRUE.equals(changed)) {
                    expired++;
                }
            } catch (Exception ex) {
                log.warn("Failed to expire payment order {}, retry next sweep", order.getId(), ex);
            }
        }
        return expired;
    }

    private PaymentOrder reconcileOrderFromItemPayments(PaymentOrder order) {
        if (!PaymentOrderStatus.UNPAID.name().equals(order.getStatus())) {
            return order;
        }
        List<PaymentOrderItem> items = listItems(order.getId());
        if (items.isEmpty()) {
            throw new BaseException("支付订单没有关联酒款");
        }
        List<EntryPayment> payments = items.stream()
                .map(item -> entryPaymentMapper.selectById(item.getEntryPaymentId()))
                .toList();
        long paidCount = payments.stream()
                .filter(Objects::nonNull)
                .filter(payment -> EntryPaymentStatus.PAID.name().equals(payment.getStatus()))
                .count();
        if (paidCount == 0) {
            return order;
        }
        if (paidCount != payments.size()) {
            throw new BaseException("订单存在部分酒款已支付，请联系组委会核对后再付款");
        }

        LocalDateTime paidTime = payments.stream()
                .map(EntryPayment::getPaidTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        String payMethod = payments.stream()
                .map(EntryPayment::getPayMethod)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(EntryPayMethod.MANUAL.name());
        order.setStatus(PaymentOrderStatus.PAID.name());
        order.setPayMethod(payMethod);
        order.setPaidAmount(order.getAmount());
        order.setPaidTime(paidTime);
        paymentOrderMapper.updateById(order);
        for (PaymentOrderItem item : items) {
            item.setStatus(EntryPaymentStatus.PAID.name());
            paymentOrderItemMapper.updateById(item);
        }
        RegistrationBatch batch = requireBatch(order.getRegistrationBatchId());
        batch.setStatus(RegistrationBatchStatus.PAID.name());
        registrationBatchMapper.updateById(batch);
        return paymentOrderMapper.selectById(order.getId());
    }

    private PaymentOrderStatusVO toStatusVO(PaymentOrder order) {
        return PaymentOrderStatusVO.builder()
                .orderId(order.getId())
                .batchId(order.getRegistrationBatchId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .payMethod(order.getPayMethod())
                .bankTransferId(order.getBankTransferId())
                .amount(order.getAmount())
                .paidAmount(order.getPaidAmount())
                .refundedAmount(order.getRefundedAmount())
                .expireTime(order.getExpireTime())
                .paidTime(order.getPaidTime())
                .build();
    }

    private String generateOutTradeNo() {
        return "BCB" + System.currentTimeMillis()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
