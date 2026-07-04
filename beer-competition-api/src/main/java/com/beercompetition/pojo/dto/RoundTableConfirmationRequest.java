package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoundTableConfirmationRequest {

    @NotNull(message = "确认版本不能为空")
    private Integer resultVersion;
}
