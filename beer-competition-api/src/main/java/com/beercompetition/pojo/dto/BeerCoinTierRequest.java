package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BeerCoinTierRequest {

    @Positive(message = "阶梯起始数量必须大于 0")
    private Long startQuantity;

    @Positive(message = "阶梯结束数量必须大于 0")
    private Long endQuantity;

    @DecimalMin(value = "0.01", message = "单枚价格必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "单枚价格最多保留两位小数")
    private BigDecimal unitPrice;
}
