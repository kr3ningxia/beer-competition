package com.beercompetition.pojo.vo;

import lombok.Data;

@Data
public class ScoreProgressStatsVO {

    private Long finalizedCount;
    private Long advancedCount;
    private Long commentWarningCount;
    private Double averageReviewMinutes;
    private Double averageReviewSeconds;
    private Double averageCommentChars;
}
