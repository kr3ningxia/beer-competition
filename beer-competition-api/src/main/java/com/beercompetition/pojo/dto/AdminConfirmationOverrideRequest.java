package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminConfirmationOverrideRequest {

    @NotBlank(message = "请填写现场确认原因")
    @Size(max = 255, message = "现场确认原因不能超过 255 字")
    private String reason;
}
