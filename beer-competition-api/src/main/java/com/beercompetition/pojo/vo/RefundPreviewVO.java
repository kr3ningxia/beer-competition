package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RefundPreviewVO {
    private Long beerEntryId;
    private Long competitionId;
    private Integer activeEntryCountBefore;
    private Integer activeEntryCountAfter;
    private BigDecimal currentPaidAmount;
    private BigDecimal refundAmount;
    private BigDecimal remainingAmountAfterRefund;
    private String pricingNote;
}
