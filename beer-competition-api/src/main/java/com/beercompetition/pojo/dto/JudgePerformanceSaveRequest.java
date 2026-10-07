package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class JudgePerformanceSaveRequest {

    @DecimalMin(value = "0.0", message = "手工评分不能小于 0")
    @DecimalMax(value = "50.0", message = "手工评分不能大于 50")
    @Digits(integer = 2, fraction = 1, message = "手工评分最多保留 1 位小数")
    private BigDecimal manualScore;

    @Size(max = 1000, message = "评价依据不能超过 1000 字")
    private String evidence;

    @NotBlank(message = "保存状态不能为空")
    @Pattern(regexp = "^(DRAFT|CONFIRMED)$", message = "保存状态不合法")
    private String status;

    @NotNull(message = "版本号不能为空")
    @Min(value = 0, message = "版本号不能小于 0")
    private Integer version;
}
