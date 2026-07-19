package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationBatchQuoteVO {

    private Long competitionId;
    private Integer entryCount;
    private BigDecimal unitAmount;
    private BigDecimal standardUnitAmount;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private Boolean earlyBirdActive;
    private LocalDateTime earlyBirdDeadline;
    private LocalDateTime quotedAt;
}
