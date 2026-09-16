package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationRuleUpdateRequest {

    @NotBlank(message = "通知类型不能为空")
    private String eventCode;
    private Boolean enabled;
    @NotBlank(message = "发送方式不能为空")
    private String scheduleMode;
    private LocalDateTime scheduledAt;
    @Min(value = -525600, message = "提前时间不能超过一年")
    @Max(value = 525600, message = "延后时间不能超过一年")
    private Integer offsetMinutes;
    @Size(min = 5, max = 5, message = "发送时间格式不正确")
    private String sendTime;
    private Long templateId;
}
