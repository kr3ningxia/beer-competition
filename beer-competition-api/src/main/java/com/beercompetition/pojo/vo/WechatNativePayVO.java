package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WechatNativePayVO {

    private String mode;
    private String outTradeNo;
    private BigDecimal amount;
    private String codeUrl;
    private LocalDateTime expireTime;
    private String paymentStatus;
}
