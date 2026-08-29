package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BeerCoinAdjustmentRequest {

    @NotNull(message = "企业账户不能为空")
    private Long enterpriseAccountId;

    @NotBlank(message = "调整方向不能为空")
    private String direction;

    @NotNull(message = "调整数量不能为空")
    @Positive(message = "调整数量必须大于 0")
    private Long quantity;

    @NotBlank(message = "调整原因不能为空")
    @Size(max = 500, message = "调整原因不能超过 500 个字符")
    private String reason;
}
