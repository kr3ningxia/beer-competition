package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NotificationTemplateUpdateRequest {

    @NotBlank(message = "邮件主题不能为空")
    @Size(max = 200, message = "邮件主题不能超过200个字符")
    private String subject;

    @NotBlank(message = "邮件正文不能为空")
    @Size(max = 50000, message = "邮件正文不能超过50000个字符")
    private String htmlBody;
}
