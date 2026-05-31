package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TableScoreFinalizeRequest {

    @Valid
    @NotEmpty(message = "共识评分维度不能为空")
    private List<DimensionRequest> dimensions;

    @NotNull(message = "共识分不能为空")
    private BigDecimal consensusScore;

    @NotBlank(message = "桌长总结不能为空")
    private String comments;

    @NotNull(message = "请标记是否晋级")
    private Boolean advanced;
}
