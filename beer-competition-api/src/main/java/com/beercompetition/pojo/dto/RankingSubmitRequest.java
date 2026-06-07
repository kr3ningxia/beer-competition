package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RankingSubmitRequest {

    @Valid
    @NotEmpty(message = "排序结果不能为空")
    private List<@NotNull(message = "排序结果项不能为空") RankingResultItemRequest> results;
}
