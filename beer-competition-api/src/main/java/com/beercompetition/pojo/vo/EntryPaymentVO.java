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
    private LocalDateTime paidTime;
}
