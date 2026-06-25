package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CompetitionAnalyticsVO {

    private LocalDateTime generatedAt;
    private CompetitionAnalyticsSummaryVO summary;
    private CompetitionAnalyticsRegistrationVO registration;
    private CompetitionAnalyticsPaymentVO payment;
    private CompetitionAnalyticsDeliveryVO delivery;
    private CompetitionAnalyticsFeedbackVO feedback;
    private List<String> warnings;
}
