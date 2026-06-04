package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EntryDetailVO {

    private Long id;
    private String uuid;
    private String labelCode;
    private String shortCode;
    private String scanToken;
    private Long competitionId;
    private String competitionCode;
    private String name;
    private String style;
    private BigDecimal abv;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String competitionName;
    private LocalDate competitionDate;
    private CompetitionLogisticsVO competitionLogistics;
    private String status;
    private BigDecimal entryFee;
    private Integer storedFlag;
    private Boolean stored;
    private String paymentStatus;
    private EntryPaymentVO payment;
    private EntryDeliveryVO delivery;
    private String deliveryMethod;
    private String deliveryStatus;
    private String carrier;
    private String trackingNo;
    private String deliveryNote;
    private LocalDateTime deliverySubmittedAt;
    private LocalDateTime deliveryReceivedAt;
    private Boolean canDownloadLabel;
    private LocalDateTime submittedAt;
    private List<EntryExtraFieldVO> extraFields;
}
