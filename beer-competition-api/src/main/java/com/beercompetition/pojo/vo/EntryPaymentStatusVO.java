package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EntryPaymentStatusVO {

    private Long entryId;
    private String entryStatus;
    private String paymentStatus;
    private Boolean canDownloadLabel;
    private LocalDateTime paidTime;
}
