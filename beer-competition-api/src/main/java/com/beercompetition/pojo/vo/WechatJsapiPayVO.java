package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WechatJsapiPayVO {

    private String mode;
    private String outTradeNo;
    private BigDecimal amount;
    private LocalDateTime expireTime;
    private String paymentStatus;
    private JsapiPayParams payParams;

    @Data
    @Builder
    public static class JsapiPayParams {
        private String appId;
        private String timeStamp;
        private String nonceStr;
        private String packageValue;
        private String signType;
        private String paySign;
    }
}
