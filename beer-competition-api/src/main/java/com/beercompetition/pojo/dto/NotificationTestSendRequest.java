package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationTestSendRequest {

    @NotBlank(message = "通知类型不能为空")
    private String eventCode;

    @NotBlank(message = "测试邮箱不能为空")
    @Email(message = "测试邮箱格式不正确")
    private String email;
}
