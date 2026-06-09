package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminEntryVO {

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
    private String status;
    private String paymentStatus;
    private String deliveryStatus;
    private Boolean stored;
    private Boolean assigned;
    private String pathText;
    private LocalDateTime submittedAt;
    private LocalDateTime paidTime;
    private LocalDateTime deliveryReceivedAt;
    private LocalDateTime lastModifiedAt;
    private Boolean canConfirmPayment;
    private Boolean canMarkStored;
    private Boolean canCancel;
    private Boolean canEdit;
    private List<AdminEntryTraceVO> traces;
}
