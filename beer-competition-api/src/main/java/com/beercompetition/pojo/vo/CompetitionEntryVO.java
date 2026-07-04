package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private String styleCategoryName;
    private String styleCode;
    private String styleDescription;
    private BigDecimal abv;
    private List<EntryExtraFieldVO> extraFields;
    private String status;
    private String paymentStatus;
    private LocalDateTime paidTime;
    private String refundStatus;
    private String refundReason;
    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundProcessedAt;
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
