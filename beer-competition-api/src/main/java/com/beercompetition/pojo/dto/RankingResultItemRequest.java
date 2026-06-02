package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RankingResultItemRequest {

    @NotNull(message = "酒款不能为空")
    private Long beerEntryId;

    @NotNull(message = "排序不能为空")
    @Min(value = 1, message = "排序必须大于 0")
    private Integer rankNo;

    private String slotLabel;
}
