package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RankingSubmitRequest {

    @Valid
    @NotEmpty(message = "排序结果不能为空")
    private List<RankingResultItemRequest> results;
}
