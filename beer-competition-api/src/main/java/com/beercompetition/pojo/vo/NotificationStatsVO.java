package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationStatsVO {

    private Integer recipientCount;
    private Integer emailRecipientCount;
    private Integer missingEmailCount;
    private Integer pendingCount;
    private Integer sentCount;
    private Integer retryingCount;
    private Integer failedCount;
    private Integer skippedCount;
}
