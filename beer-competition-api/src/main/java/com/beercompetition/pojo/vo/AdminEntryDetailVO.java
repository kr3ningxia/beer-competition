package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminEntryDetailVO {

    private Long id;
    private String uuid;
    private String labelCode;
    private String shortCode;
    private String scanToken;
    private Long competitionId;
    private String competitionName;
    private String competitionCode;
    private String name;
    private String breweryCompanyName;
    private String breweryContactName;
    private Long categoryId;
    private String categoryName;
    private String style;
    private BigDecimal abv;
    private String description;
    private String status;
    private String paymentStatus;
    private EntryPaymentVO payment;
    private String deliveryStatus;
    private EntryDeliveryVO delivery;
    private Boolean stored;
    private Boolean assigned;
    private Boolean resultPublished;
    private Boolean canConfirmPayment;
    private Boolean canMarkStored;
    private Boolean canCancel;
    private Boolean canEdit;
    private LocalDateTime submittedAt;
    private List<EntryExtraFieldVO> extraFields;
    private List<AdminEntryTraceVO> traces;
    private List<AdminEntryLogVO> logs;
}
