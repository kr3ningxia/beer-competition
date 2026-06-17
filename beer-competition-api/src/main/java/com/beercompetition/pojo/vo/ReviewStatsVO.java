package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewStatsVO {

    private Integer submittedCount;
    private Integer averageDurationSeconds;
    private Integer averageCommentChars;
    private Integer siteAverageDurationSeconds;
    private Integer siteAverageCommentChars;
    private Integer commentMinLength;
}
