package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortalEntryRefundRequest {

    @NotBlank(message = "请填写退款原因")
    @Size(max = 300, message = "退款原因不能超过300个字符")
    private String reason;
}
