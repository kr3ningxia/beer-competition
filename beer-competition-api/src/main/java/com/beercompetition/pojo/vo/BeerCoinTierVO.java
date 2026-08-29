package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BeerCoinTierVO {

    private Long id;
    private Long startQuantity;
    private Long endQuantity;
    private BigDecimal unitPrice;
    private Integer sortOrder;
}
