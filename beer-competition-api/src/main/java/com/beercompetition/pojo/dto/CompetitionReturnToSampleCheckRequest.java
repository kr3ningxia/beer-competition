package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompetitionReturnToSampleCheckRequest {

    @NotBlank(message = "请填写退回原因")
    @Size(max = 120, message = "退回原因不能超过120个字符")
    private String reason;
}
