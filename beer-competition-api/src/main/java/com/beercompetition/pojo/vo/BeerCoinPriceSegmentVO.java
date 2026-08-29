package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BeerCoinPriceSegmentVO {

    private Long startQuantity;
    private Long endQuantity;
    private Long quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
