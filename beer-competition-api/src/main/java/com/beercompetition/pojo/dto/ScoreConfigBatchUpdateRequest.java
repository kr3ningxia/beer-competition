package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ScoreConfigBatchUpdateRequest {

    @Valid
    @NotEmpty(message = "评分配置不能为空")
    private List<ScoreConfigItemRequest> configs;
}
