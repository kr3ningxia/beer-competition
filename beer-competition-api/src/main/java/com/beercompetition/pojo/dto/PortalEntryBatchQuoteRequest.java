package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PortalEntryBatchQuoteRequest {

    @NotNull(message = "酒款数量不能为空")
    @Min(value = 1, message = "请至少添加一款参赛酒")
    @Max(value = 20, message = "一次最多提交20款参赛酒")
    private Integer entryCount;
}
