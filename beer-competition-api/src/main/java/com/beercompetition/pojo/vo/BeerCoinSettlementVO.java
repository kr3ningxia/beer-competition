package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BeerCoinSettlementVO {

    private Boolean applicable;
    private Integer effectiveEntryCount;
    private Long requiredQuantity;
    private Long chargedQuantity;
    private Long pendingQuantity;
    private Boolean completed;
    private String status;
}
