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

    /**
     * 折后单价（元/款）。阶梯价以金额为准，折扣率由服务端按报名费换算。
     */
    @DecimalMin(value = "0.01", message = "折后单价不能低于 0.01 元")
    @Digits(integer = 8, fraction = 2, message = "折后单价最多保留 2 位小数")
    private BigDecimal price;

    /**
     * 兼容旧口径：只传折扣率时按报名费换算折后单价。
     */
    @DecimalMin(value = "0.01", message = "折扣率不能低于 1%")
    @DecimalMax(value = "1.00", message = "折扣率不能高于 100%")
    @Digits(integer = 1, fraction = 6, message = "折扣率最多保留 6 位小数")
    private BigDecimal discountRate;
}
