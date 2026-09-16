package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailDeliveryVO {

    private Long id;
    private String eventCode;
    private String eventLabel;
    private String recipientName;
    private String recipientEmail;
    private String subject;
    private String status;
    private String statusLabel;
    private Integer attemptCount;
    private LocalDateTime scheduledTime;
    private LocalDateTime sentTime;
    private String lastError;
}
