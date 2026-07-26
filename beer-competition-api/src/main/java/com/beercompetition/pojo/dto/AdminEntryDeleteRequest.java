package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminEntryDeleteRequest {

    @NotBlank(message = "请填写删除原因")
    @Size(max = 500, message = "删除原因不能超过 500 个字符")
    private String reason;

    @NotBlank(message = "请输入酒款短编号确认")
    private String confirmationCode;

    private String paymentDisposition;

    private Boolean highRiskConfirmed;
}
