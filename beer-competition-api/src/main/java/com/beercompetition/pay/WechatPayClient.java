package com.beercompetition.pay;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface WechatPayClient {

    NativePayResult createNativePayment(NativePayRequest request);

    PaymentQueryResult queryPayment(String outTradeNo);

    void closePayment(String outTradeNo);

    RefundResult createRefund(RefundRequest request);

    RefundResult queryRefund(String outRefundNo);

    PaymentNotifyResult parsePaymentNotify(WechatNotifyRequest request);

    RefundNotifyResult parseRefundNotify(WechatNotifyRequest request);

    record NativePayRequest(
            String outTradeNo,
            String description,
            BigDecimal amount,
            LocalDateTime expireTime
    ) {
    }

    record NativePayResult(String codeUrl) {
    }

    record PaymentQueryResult(
            String outTradeNo,
            String transactionId,
            String tradeState,
            String tradeStateDesc,
            BigDecimal paidAmount,
            LocalDateTime paidTime
    ) {
    }

    record RefundRequest(
            String outTradeNo,
            String transactionId,
            String outRefundNo,
            String reason,
            BigDecimal refundAmount,
            BigDecimal totalAmount
    ) {
    }

    record RefundResult(
            String outTradeNo,
            String outRefundNo,
            String refundId,
            String refundStatus,
            LocalDateTime successTime
    ) {
    }

    record WechatNotifyRequest(
            String serial,
            String timestamp,
            String nonce,
            String signature,
            String body
    ) {
    }

    record PaymentNotifyResult(
            String notifyId,
            String eventType,
            String outTradeNo,
            String transactionId,
            String tradeState,
            String tradeStateDesc,
            BigDecimal paidAmount,
            LocalDateTime paidTime,
            String rawJson
    ) {
    }

    record RefundNotifyResult(
            String notifyId,
            String eventType,
            String outTradeNo,
            String outRefundNo,
            String refundId,
            String refundStatus,
            LocalDateTime successTime,
            String rawJson
    ) {
    }
}
