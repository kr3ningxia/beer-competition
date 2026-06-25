package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionAnalyticsSummaryVO {

    private Integer totalEntries;
    private Integer registeredEntries;
    private Integer storedEntries;
    private Integer paidEntries;
    private Integer pendingPaymentEntries;
    private Integer receivedEntries;
    private Integer reviewedEntries;
    private Integer scoreRecords;
    private Integer awardCount;
    private Integer testRecordCount;
    private Integer averageCommentChars;
    private Integer averageReviewSeconds;
}
