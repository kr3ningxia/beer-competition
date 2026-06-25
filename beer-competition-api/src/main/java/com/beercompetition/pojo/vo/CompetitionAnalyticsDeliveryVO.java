package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompetitionAnalyticsDeliveryVO {

    private List<CompetitionAnalyticsBucketVO> statuses;
    private List<CompetitionAnalyticsBucketVO> methods;
    private List<CompetitionAnalyticsSampleVO> pendingEntries;
}
