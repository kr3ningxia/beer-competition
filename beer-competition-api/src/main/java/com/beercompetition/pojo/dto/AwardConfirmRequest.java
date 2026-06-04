package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AwardConfirmRequest {

    @Valid
    @NotEmpty(message = "奖项结果不能为空")
    private List<AwardConfirmItemRequest> awards;
}
