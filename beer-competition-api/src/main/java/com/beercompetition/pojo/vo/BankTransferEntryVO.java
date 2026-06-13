package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BankTransferEntryVO {

    private Long entryId;
    private Long paymentId;
    private String entryName;
    private String shortCode;
    private String categoryName;
    private String style;
    private BigDecimal amount;
}
