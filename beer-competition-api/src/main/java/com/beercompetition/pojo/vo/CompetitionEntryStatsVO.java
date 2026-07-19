package com.beercompetition.pojo.vo;

import lombok.Data;

@Data
public class CompetitionEntryStatsVO {

    private Long competitionId;
    private Long totalCount;
    private Long pendingPaymentCount;
    private Long registeredCount;
    private Long storedCount;
    private Long canceledCount;
    private Long resultPublishedCount;
}
