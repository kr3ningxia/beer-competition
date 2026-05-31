package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DimensionRequest {

    @NotBlank(message = "维度 key 不能为空")
    private String key;

    @NotBlank(message = "维度名称不能为空")
    private String label;

    private BigDecimal score;

    @NotNull(message = "满分不能为空")
    private BigDecimal maxScore;
}
