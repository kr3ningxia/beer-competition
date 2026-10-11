package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoundTableConfirmationRequest {

    /** 组别独立确认维度；旧单组别请求可为空。 */
    private Long categoryId;

    @NotNull(message = "确认版本不能为空")
    private Integer resultVersion;
}
