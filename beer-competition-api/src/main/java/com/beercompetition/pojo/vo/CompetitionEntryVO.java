package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CompetitionEntryVO {

    private Long id;
    private String uuid;
    private String labelCode;
    private String shortCode;
    private String scanToken;
    private String name;
    private String breweryCompanyName;
    private String breweryContactName;
    private Long categoryId;
    private String categoryName;
    private String style;
    private String description;
    private String styleCategoryName;
    private String styleCode;
    private String styleDescription;
    private String status;
    private String paymentStatus;
    private LocalDateTime paidTime;
    private String deliveryMethod;
    private String deliveryStatus;
    private String carrier;
    private String trackingNo;
    private LocalDateTime deliverySubmittedAt;
    private Boolean stored;
    private Boolean advanced;
    private Boolean canConfirmPayment;
    private Boolean canMarkStored;
    private Boolean canCancel;
    private String sourceTable;
    private String sourceResult;
}
