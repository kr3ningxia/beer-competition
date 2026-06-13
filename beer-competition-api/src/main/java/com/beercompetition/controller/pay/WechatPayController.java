package com.beercompetition.controller.pay;

import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.service.WechatPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pay/wechat")
public class WechatPayController {

    private static final Map<String, String> SUCCESS_RESPONSE = Map.of("code", "SUCCESS", "message", "成功");

    private final WechatPaymentService wechatPaymentService;

    @PostMapping("/payment-notify")
    public ResponseEntity<Map<String, String>> paymentNotify(
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestBody(required = false) String body) {
        wechatPaymentService.handlePaymentNotify(new WechatPayClient.WechatNotifyRequest(
                serial, timestamp, nonce, signature, body));
        return ResponseEntity.ok(SUCCESS_RESPONSE);
    }

    @PostMapping("/refund-notify")
    public ResponseEntity<Map<String, String>> refundNotify(
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestBody(required = false) String body) {
        wechatPaymentService.handleRefundNotify(new WechatPayClient.WechatNotifyRequest(
                serial, timestamp, nonce, signature, body));
        return ResponseEntity.ok(SUCCESS_RESPONSE);
    }
}
