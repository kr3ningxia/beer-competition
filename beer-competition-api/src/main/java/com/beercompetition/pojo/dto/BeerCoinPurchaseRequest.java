package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BeerCoinPurchaseRequest {

    @NotNull(message = "购买数量不能为空")
    @Positive(message = "购买数量必须大于 0")
    private Long quantity;
}
