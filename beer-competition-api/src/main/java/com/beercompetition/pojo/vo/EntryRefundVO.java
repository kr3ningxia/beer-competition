package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EntryRefundVO {

    private Long id;
    private Long beerEntryId;
    private Long entryPaymentId;
    private String refundNo;
    private BigDecimal amount;
    private String status;
    private String reason;
    private Long requestedByPortalId;
    private LocalDateTime requestedTime;
    private Long processedByAdminId;
    private LocalDateTime processedTime;
    private LocalDateTime successTime;
    private String failReason;
    private String wechatRefundId;
    private String wechatRefundStatus;
    private String outRefundNo;
}
