package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentOrderStatusVO {

    private Long orderId;
    private Long batchId;
    private String orderNo;
    private String status;
    private String payMethod;
    private Long bankTransferId;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal refundedAmount;
    private LocalDateTime expireTime;
    private LocalDateTime paidTime;
}
