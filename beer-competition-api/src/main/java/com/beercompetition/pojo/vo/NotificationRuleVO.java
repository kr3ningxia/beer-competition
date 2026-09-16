package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationRuleVO {

    private Long id;
    private String eventCode;
    private String eventLabel;
    private Boolean enabled;
    private String scheduleMode;
    private LocalDateTime scheduledAt;
    private Integer offsetMinutes;
    private String sendTime;
    private String timezone;
    private Long templateId;
    private Integer templateVersion;
    private String nextTriggerDescription;
}
