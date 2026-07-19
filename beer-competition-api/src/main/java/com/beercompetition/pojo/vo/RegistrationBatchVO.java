package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RegistrationBatchVO {

    private Long id;
    private String batchNo;
    private Long competitionId;
    private String competitionName;
    private Integer entryCount;
    private BigDecimal totalAmount;
    private String status;
    private Long paymentOrderId;
    private String paymentOrderNo;
    private String paymentStatus;
    private String payMethod;
    private BigDecimal paidAmount;
    private BigDecimal refundedAmount;
    private LocalDateTime expireTime;
    private LocalDateTime paidTime;
    private LocalDateTime submittedAt;
    private List<EntryDetailVO> entries;
}
