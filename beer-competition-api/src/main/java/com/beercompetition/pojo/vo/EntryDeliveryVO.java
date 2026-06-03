package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EntryDeliveryVO {

    private String deliveryMethod;
    private String carrier;
    private String trackingNo;
    private String deliveryNote;
    private String deliveryStatus;
    private LocalDateTime submittedTime;
    private LocalDateTime receivedTime;
}
