package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EntryPaymentVO {

    private String status;
    private String payMethod;
    private BigDecimal amount;
    private String outTradeNo;
    private String wechatTransactionId;
    private Long bankTransferId;
    private String codeUrl;
    private LocalDateTime expireTime;
    private BigDecimal paidAmount;
    private String wechatTradeState;
    private String wechatTradeStateDesc;
    private LocalDateTime paidTime;
}
