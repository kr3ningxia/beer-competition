package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EntrySummaryVO {

    private Long id;
    private String uuid;
    private String labelCode;
    private String shortCode;
    private String scanToken;
    private Long competitionId;
    private String competitionType;
    private String competitionCode;
    private String name;
    private String competitionName;
    private CompetitionLogisticsVO competitionLogistics;
    private Long categoryId;
    private String categoryName;
    private String style;
    private String status;
    private Boolean published;
    private BigDecimal abv;
    private BigDecimal entryFee;
    private Integer storedFlag;
    private String paymentStatus;
    private EntryPaymentVO payment;
    private EntryRefundVO refund;
    private String refundStatus;
    private String refundReason;
    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundProcessedAt;
    private Boolean canRequestRefund;
    private EntryDeliveryVO delivery;
    private String deliveryMethod;
    private String deliveryStatus;
    private String carrier;
    private String trackingNo;
    private Boolean canDownloadLabel;
    private LocalDateTime submittedAt;
    private Integer total;
    private Integer pendingPayment;
    private Integer registered;
    private Integer stored;
    private Integer canceled;
    private Integer resultPublished;
}
