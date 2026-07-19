package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PortalEntryBatchSubmitRequest {

    @NotBlank(message = "报名请求标识不能为空")
    @Size(max = 64, message = "报名请求标识不能超过64个字符")
    private String idempotencyKey;

    private Boolean rulesAccepted;

    private BigDecimal expectedTotalAmount;

    @Valid
    @NotEmpty(message = "请至少添加一款参赛酒")
    @Size(max = 20, message = "一次最多提交20款参赛酒")
    private List<PortalEntrySubmitRequest> entries = new ArrayList<>();
}
