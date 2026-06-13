package com.beercompetition.pay;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.properties.WechatPayProperties;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.wechat-pay", name = "mode", havingValue = "WECHAT")
public class WechatSdkPayClient implements WechatPayClient {

    private static final String CURRENCY_CNY = "CNY";
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final WechatPayProperties properties;
    private final ObjectMapper objectMapper;

    private volatile NativePayService nativePayService;
    private volatile RefundService refundService;
    private volatile NotificationParser notificationParser;

    @Override
    public NativePayResult createNativePayment(NativePayRequest request) {
        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(required(properties.getAppId(), "微信支付 AppID 未配置"));
        prepayRequest.setMchid(required(properties.getMchId(), "微信支付商户号未配置"));
        prepayRequest.setDescription(request.description());
        prepayRequest.setOutTradeNo(request.outTradeNo());
        prepayRequest.setTimeExpire(toWechatTime(request.expireTime()));
        prepayRequest.setNotifyUrl(notifyUrl("/api/pay/wechat/payment-notify"));

        com.wechat.pay.java.service.payments.nativepay.model.Amount amount =
                new com.wechat.pay.java.service.payments.nativepay.model.Amount();
        amount.setTotal(Math.toIntExact(yuanToCents(request.amount())));
        amount.setCurrency(CURRENCY_CNY);
        prepayRequest.setAmount(amount);

        PrepayResponse response = nativePay().prepay(prepayRequest);
        return new NativePayResult(response.getCodeUrl());
    }

    @Override
    public PaymentQueryResult queryPayment(String outTradeNo) {
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(required(properties.getMchId(), "微信支付商户号未配置"));
        request.setOutTradeNo(outTradeNo);
        Transaction transaction = nativePay().queryOrderByOutTradeNo(request);
        String tradeState = transaction.getTradeState() == null ? null : transaction.getTradeState().name();
        BigDecimal paidAmount = transaction.getAmount() == null || transaction.getAmount().getTotal() == null
                ? null
                : centsToYuan(transaction.getAmount().getTotal());
        return new PaymentQueryResult(
                transaction.getOutTradeNo(),
                transaction.getTransactionId(),
                tradeState,
                transaction.getTradeStateDesc(),
                paidAmount,
                parseWechatTime(transaction.getSuccessTime())
        );
    }

    @Override
    public void closePayment(String outTradeNo) {
        CloseOrderRequest request = new CloseOrderRequest();
        request.setMchid(required(properties.getMchId(), "微信支付商户号未配置"));
        request.setOutTradeNo(outTradeNo);
        nativePay().closeOrder(request);
    }

    @Override
    public RefundResult createRefund(RefundRequest request) {
        CreateRequest createRequest = new CreateRequest();
        if (StringUtils.hasText(request.transactionId())) {
            createRequest.setTransactionId(request.transactionId());
        } else {
            createRequest.setOutTradeNo(request.outTradeNo());
        }
        createRequest.setOutRefundNo(request.outRefundNo());
        createRequest.setReason(request.reason());
        createRequest.setNotifyUrl(notifyUrl("/api/pay/wechat/refund-notify"));

        AmountReq amount = new AmountReq();
        amount.setRefund(yuanToCents(request.refundAmount()));
        amount.setTotal(yuanToCents(request.totalAmount()));
        amount.setCurrency(CURRENCY_CNY);
        createRequest.setAmount(amount);

        return toRefundResult(refund().create(createRequest));
    }

    @Override
    public RefundResult queryRefund(String outRefundNo) {
        QueryByOutRefundNoRequest request = new QueryByOutRefundNoRequest();
        request.setOutRefundNo(outRefundNo);
        return toRefundResult(refund().queryByOutRefundNo(request));
    }

    @Override
    public PaymentNotifyResult parsePaymentNotify(WechatNotifyRequest request) {
        Transaction transaction = parser().parse(toRequestParam(request), Transaction.class);
        String body = request.body();
        JsonNode root = readBody(body);
        String tradeState = transaction.getTradeState() == null ? null : transaction.getTradeState().name();
        BigDecimal paidAmount = transaction.getAmount() == null || transaction.getAmount().getTotal() == null
                ? null
                : centsToYuan(transaction.getAmount().getTotal());
        return new PaymentNotifyResult(
                text(root, "id"),
                text(root, "event_type"),
                transaction.getOutTradeNo(),
                transaction.getTransactionId(),
                tradeState,
                transaction.getTradeStateDesc(),
                paidAmount,
                parseWechatTime(transaction.getSuccessTime()),
                body
        );
    }

    @Override
    public RefundNotifyResult parseRefundNotify(WechatNotifyRequest request) {
        RefundNotification notification = parser().parse(toRequestParam(request), RefundNotification.class);
        String body = request.body();
        JsonNode root = readBody(body);
        Status status = notification.getRefundStatus();
        return new RefundNotifyResult(
                text(root, "id"),
                text(root, "event_type"),
                notification.getOutTradeNo(),
                notification.getOutRefundNo(),
                notification.getRefundId(),
                status == null ? null : status.name(),
                parseWechatTime(notification.getSuccessTime()),
                body
        );
    }

    private NativePayService nativePay() {
        if (nativePayService == null) {
            synchronized (this) {
                if (nativePayService == null) {
                    nativePayService = new NativePayService.Builder().config(config()).build();
                }
            }
        }
        return nativePayService;
    }

    private RefundService refund() {
        if (refundService == null) {
            synchronized (this) {
                if (refundService == null) {
                    refundService = new RefundService.Builder().config(config()).build();
                }
            }
        }
        return refundService;
    }

    private NotificationParser parser() {
        if (notificationParser == null) {
            synchronized (this) {
                if (notificationParser == null) {
                    notificationParser = new NotificationParser(config());
                }
            }
        }
        return notificationParser;
    }

    private RSAAutoCertificateConfig config() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(required(properties.getMchId(), "微信支付商户号未配置"))
                .privateKeyFromPath(required(properties.getPrivateKeyPath(), "微信支付商户私钥路径未配置"))
                .merchantSerialNumber(required(properties.getMerchantSerialNo(), "微信支付商户证书序列号未配置"))
                .apiV3Key(required(properties.getApiV3Key(), "微信支付 API v3 密钥未配置"))
                .build();
    }

    private RequestParam toRequestParam(WechatNotifyRequest request) {
        return new RequestParam.Builder()
                .serialNumber(request.serial())
                .timestamp(request.timestamp())
                .nonce(request.nonce())
                .signature(request.signature())
                .body(request.body())
                .build();
    }

    private RefundResult toRefundResult(Refund refund) {
        String status = refund.getStatus() == null ? null : refund.getStatus().name();
        return new RefundResult(
                refund.getOutTradeNo(),
                refund.getOutRefundNo(),
                refund.getRefundId(),
                status,
                parseWechatTime(refund.getSuccessTime())
        );
    }

    private String notifyUrl(String path) {
        String baseUrl = required(properties.getNotifyBaseUrl(), "微信支付回调域名未配置");
        return baseUrl.replaceAll("/+$", "") + path;
    }

    private String toWechatTime(LocalDateTime time) {
        return time.atZone(CHINA_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private LocalDateTime parseWechatTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return OffsetDateTime.parse(value).atZoneSameInstant(CHINA_ZONE).toLocalDateTime();
    }

    private Long yuanToCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    private BigDecimal centsToYuan(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2);
    }

    private String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
        }
        return value.trim();
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
}
