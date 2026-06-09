package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CompetitionLogisticsVO {

    private String deliveryMethod;
    private LocalDateTime sampleArrivalStart;
    private LocalDateTime sampleArrivalDeadline;
    private String sampleQuantityNote;
    private String deliveryRecipient;
    private String deliveryPhone;
    private String deliveryAddress;
    private String deliveryNote;
    private String logisticsVisibility;
}
