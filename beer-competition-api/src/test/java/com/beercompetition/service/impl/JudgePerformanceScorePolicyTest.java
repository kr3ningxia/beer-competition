package com.beercompetition.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JudgePerformanceScorePolicyTest {

    @Test
    void topTenPercentReceivesFullCommentScore() {
        double percentile = JudgePerformanceScorePolicy.percentile(11, 1);

        assertThat(percentile).isEqualTo(0.9);
        assertThat(JudgePerformanceScorePolicy.topPercent(percentile)).isEqualTo(10);
        assertThat(JudgePerformanceScorePolicy.commentScore(percentile, true)).isEqualTo(30);
    }

    @Test
    void incompleteTasksReceiveNoCommentScore() {
        assertThat(JudgePerformanceScorePolicy.commentScore(1, false)).isZero();
    }

    @Test
    void roleMinimumsNormalizeCommentEffortBeforeComparison() {
        double crossRatio = JudgePerformanceScorePolicy.normalizedCommentLength(
                50, JudgePerformanceScorePolicy.minimumLength("CROSS", null));
        double professionalRatio = JudgePerformanceScorePolicy.normalizedCommentLength(
                30, JudgePerformanceScorePolicy.minimumLength("PROFESSIONAL", null));

        assertThat(crossRatio).isEqualTo(1);
        assertThat(professionalRatio).isEqualTo(1);
    }

    @Test
    void taskVolumeDoesNotIncreaseAverageCommentEffort() {
        assertThat(JudgePerformanceScorePolicy.averageRequirementRatio(4, 4))
                .isEqualTo(JudgePerformanceScorePolicy.averageRequirementRatio(1, 1));
    }
}
