package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CompetitionFeeTierVO {
    private Long id;
    private Integer startQuantity;
    private BigDecimal discountRate;
    private Integer sortOrder;
    private Integer enabled;
}
