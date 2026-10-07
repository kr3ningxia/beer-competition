package com.beercompetition.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JudgePerformanceScorePolicyTest {

    @Test
    void commentScoreUsesFiveQuantilesWithTenPointGradient() {
        double percentile = JudgePerformanceScorePolicy.percentile(11, 1);

        assertThat(percentile).isEqualTo(0.9);
        assertThat(JudgePerformanceScorePolicy.topPercent(percentile)).isEqualTo(10);
        assertThat(JudgePerformanceScorePolicy.commentScore(0.8, true)).isEqualTo(50);
        assertThat(JudgePerformanceScorePolicy.commentScore(0.79, true)).isEqualTo(40);
        assertThat(JudgePerformanceScorePolicy.commentScore(0.6, true)).isEqualTo(40);
        assertThat(JudgePerformanceScorePolicy.commentScore(0.59, true)).isEqualTo(30);
        assertThat(JudgePerformanceScorePolicy.commentScore(0.4, true)).isEqualTo(30);
        assertThat(JudgePerformanceScorePolicy.commentScore(0.39, true)).isEqualTo(20);
        assertThat(JudgePerformanceScorePolicy.commentScore(0.2, true)).isEqualTo(10);
        assertThat(JudgePerformanceScorePolicy.commentScore(0, true)).isEqualTo(10);
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
