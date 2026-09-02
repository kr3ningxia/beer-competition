package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class QuotePriceItemVO {
    private Integer sequence;
    private BigDecimal baseAmount;
    private BigDecimal discountRate;
    private BigDecimal amount;
}
