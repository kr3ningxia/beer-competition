package com.beercompetition.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DimensionRequest {

    @NotBlank(message = "维度 key 不能为空")
    private String key;

    @NotBlank(message = "维度名称不能为空")
    private String label;

    private BigDecimal score;

    @NotNull(message = "满分不能为空")
    @Positive(message = "满分必须大于 0")
    private BigDecimal maxScore;

    private String notePrompt;
}
