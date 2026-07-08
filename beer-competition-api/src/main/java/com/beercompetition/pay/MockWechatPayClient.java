package com.beercompetition.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.wechat-pay", name = "mode", havingValue = "MOCK", matchIfMissing = true)
public class MockWechatPayClient implements WechatPayClient {

    private final ObjectMapper objectMapper;

    @Override
    public NativePayResult createNativePayment(NativePayRequest request) {
        return new NativePayResult("mock://wechat-pay/native/" + request.outTradeNo());
    }

    @Override
    public JsapiPayResult createJsapiPayment(JsapiPayRequest request) {
        return new JsapiPayResult(
                "mock-app-id",
                String.valueOf(System.currentTimeMillis() / 1000),
                "mock-nonce",
                "prepay_id=mock-" + request.outTradeNo(),
                "RSA",
                "mock-pay-sign"
        );
    }

    @Override
    public PaymentQueryResult queryPayment(String outTradeNo) {
        return new PaymentQueryResult(outTradeNo, null, "NOTPAY", "测试订单待支付", null, null);
    }

    @Override
    public void closePayment(String outTradeNo) {
        // MOCK 模式无需远端关单。
    }

    @Override
    public RefundResult createRefund(RefundRequest request) {
        return new RefundResult(request.outTradeNo(), request.outRefundNo(),
                "MOCK-RF-" + request.outRefundNo(), "SUCCESS", LocalDateTime.now());
    }

    @Override
    public RefundResult queryRefund(String outRefundNo) {
        return new RefundResult(null, outRefundNo, "MOCK-RF-" + outRefundNo, "SUCCESS", LocalDateTime.now());
    }

    @Override
    public PaymentNotifyResult parsePaymentNotify(WechatNotifyRequest request) {
        JsonNode root = readBody(request.body());
        String outTradeNo = text(root, "out_trade_no");
        BigDecimal paidAmount = centsToYuan(root.path("amount").path("total").asLong(0));
        return new PaymentNotifyResult(
                textOrDefault(root, "id", "MOCK-PAY-" + outTradeNo),
                textOrDefault(root, "event_type", "TRANSACTION.SUCCESS"),
                outTradeNo,
                textOrDefault(root, "transaction_id", "MOCK-TX-" + outTradeNo),
                textOrDefault(root, "trade_state", "SUCCESS"),
                textOrDefault(root, "trade_state_desc", "支付成功"),
                paidAmount,
                LocalDateTime.now(),
                request.body()
        );
    }

    @Override
    public RefundNotifyResult parseRefundNotify(WechatNotifyRequest request) {
        JsonNode root = readBody(request.body());
        String outRefundNo = text(root, "out_refund_no");
        return new RefundNotifyResult(
                textOrDefault(root, "id", "MOCK-REFUND-" + outRefundNo),
                textOrDefault(root, "event_type", "REFUND.SUCCESS"),
                text(root, "out_trade_no"),
                outRefundNo,
                textOrDefault(root, "refund_id", "MOCK-RF-" + outRefundNo),
                textOrDefault(root, "refund_status", "SUCCESS"),
                LocalDateTime.now(),
                request.body()
        );
    }

    private JsonNode readBody(String body) {
        try {
            return objectMapper.readTree(StringUtils.hasText(body) ? body : "{}");
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String text(JsonNode root, String fieldName) {
        JsonNode value = root.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String textOrDefault(JsonNode root, String fieldName, String defaultValue) {
        String value = text(root, fieldName);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private BigDecimal centsToYuan(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2);
    }
}
