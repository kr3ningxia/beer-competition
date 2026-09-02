package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompetitionFeeTierRequest {
    @NotNull(message = "阶梯起始数量不能为空")
    @Positive(message = "阶梯起始数量必须大于 0")
    private Integer startQuantity;

    @NotNull(message = "阶梯折扣不能为空")
    @DecimalMin(value = "0.01", message = "阶梯折扣必须大于 0")
    @DecimalMax(value = "1.00", message = "阶梯折扣不能超过 1")
    @Digits(integer = 1, fraction = 4, message = "阶梯折扣最多保留 4 位小数")
    private BigDecimal discountRate;
}
