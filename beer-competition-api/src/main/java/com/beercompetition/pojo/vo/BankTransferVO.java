package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BankTransferVO {

    private Long id;
    private String transferNo;
    private Long competitionId;
    private Long paymentOrderId;
    private String batchNo;
    private String competitionName;
    private String competitionCode;
    private Long breweryId;
    private String breweryName;
    private Long portalAccountId;
    private Long entryId;
    private Long paymentId;
    private String entryName;
    private String shortCode;
    private String categoryName;
    private String style;
    private BigDecimal amount;
    private Integer entryCount;
    private String payerName;
    private LocalDateTime transferTime;
    private String remark;
    private Long voucherAssetId;
    private String voucherFileName;
    private String voucherPublicUrl;
    private String status;
    private Long adminId;
    private String adminNote;
    private LocalDateTime submittedTime;
    private LocalDateTime processedTime;
}
