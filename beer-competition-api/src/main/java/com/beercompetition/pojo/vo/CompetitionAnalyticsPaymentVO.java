package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompetitionAnalyticsPaymentVO {

    private List<CompetitionAnalyticsBucketVO> channels;
    private List<CompetitionAnalyticsBucketVO> statuses;
    private Integer testRecordCount;
    private List<String> notes;
}
