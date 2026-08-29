package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BeerCoinPurchasePaymentVO {

    private String mode;
    private Long orderId;
    private String orderNo;
    private String outTradeNo;
    private BigDecimal amount;
    private String codeUrl;
    private LocalDateTime expireTime;
    private String paymentStatus;
    private WechatJsapiPayVO.JsapiPayParams payParams;
}
