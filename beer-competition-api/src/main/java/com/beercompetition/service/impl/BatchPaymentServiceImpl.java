package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.PaymentOrderItemMapper;
import com.beercompetition.mapper.PaymentOrderMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.RegistrationBatchMapper;
import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.properties.WechatPayProperties;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.PaymentOrderStatus;
import com.beercompetition.pojo.enums.RegistrationBatchStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.PaymentOrder;
import com.beercompetition.pojo.po.PaymentOrderItem;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.RegistrationBatch;
import com.beercompetition.pojo.vo.PaymentOrderStatusVO;
import com.beercompetition.pojo.vo.WechatJsapiPayVO;
import com.beercompetition.pojo.vo.WechatNativePayVO;
import com.beercompetition.service.BatchPaymentService;
import com.beercompetition.service.WechatOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BatchPaymentServiceImpl implements BatchPaymentService {

    private static final int NATIVE_PAY_EXPIRE_MINUTES = 30;

    private final WechatPayClient wechatPayClient;
    private final WechatPayProperties wechatPayProperties;
    private final WechatOAuthService wechatOAuthService;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentOrderItemMapper paymentOrderItemMapper;
    private final RegistrationBatchMapper registrationBatchMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final PortalAccountMapper portalAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatNativePayVO createNativePayment(Long orderId) {
        // 1) 校验订单归属和支付资格
        PaymentOrder order = requireOwnedOrderForUpdate(orderId);
        order = reconcileOrderFromItemPayments(order);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            return toNativePayVO(order);
        }
        if (PaymentOrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())) {
            throw new BaseException("银行转账信息已提交，请等待组委会核对到账");
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
        order = reconcileOrderFromItemPayments(order);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            return toJsapiPayVO(order, null);
        }
        if (PaymentOrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())) {
            throw new BaseException("银行转账信息已提交，请等待组委会核对到账");
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
        PaymentOrder order = requireOwnedOrder(orderId);
        order = reconcileOrderFromItemPayments(order);

        // 2) 微信回调延迟时主动查单补偿
        order = synchronizeWechatPayment(order, false);

        // 3) 返回聚合订单状态
        return toStatusVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderStatusVO simulatePayment(Long orderId) {
        // 1) 限制模拟支付只在非真实微信模式使用
        if (wechatPayProperties.isWechatMode()) {
            throw new BaseException("当前已启用微信支付，请扫码完成报名费支付");
        }
        PaymentOrder order = requireOwnedOrder(orderId);
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            return toStatusVO(order);
        }
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
    public boolean applyWechatPaymentSuccess(WechatPayClient.PaymentNotifyResult result) {
        // 1) 按微信商户单号识别聚合订单
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOutTradeNo, result.outTradeNo())
                .last("LIMIT 1 FOR UPDATE"));
        if (order == null) {
            return false;
        }
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
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
        for (PaymentOrderItem item : items) {
            EntryPayment payment = entryPaymentMapper.selectById(item.getEntryPaymentId());
            BeerEntry entry = beerEntryMapper.selectById(item.getBeerEntryId());
            if (payment == null || entry == null) {
                throw new BaseException("聚合订单酒款数据异常");
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
        paymentOrderMapper.updateById(order);
        return order;
    }

    private void closeExistingWechatOrder(PaymentOrder order) {
        if (!StringUtils.hasText(order.getOutTradeNo())) {
            return;
        }
        wechatPayClient.closePayment(order.getOutTradeNo());
        order.setOutTradeNo(null);
        order.setCodeUrl(null);
        order.setExpireTime(null);
        order.setWechatTradeState("CLOSED");
        order.setWechatTradeStateDesc("已更换付款方式");
        order.setLastQueryTime(LocalDateTime.now());
        paymentOrderMapper.updateById(order);
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
        applyPaymentSuccess(order, payMethod, null, order.getAmount(), paidTime, null);
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
