package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompetitionAnalyticsRegistrationVO {

    private List<CompetitionAnalyticsBucketVO> categories;
    private List<CompetitionAnalyticsBucketVO> styles;
    private List<CompetitionAnalyticsBucketVO> abvBuckets;
    private List<CompetitionAnalyticsBucketVO> breweries;
}
