package com.beercompetition.service.impl;

final class JudgePerformanceScorePolicy {

    private static final int COMMENT_SCORE_MAX = 50;
    private static final int COMMENT_SCORE_MIN = 10;
    private static final int COMMENT_SCORE_STEP = 10;
    private static final int DEFAULT_CROSS_MIN = 50;
    private static final int DEFAULT_OTHER_MIN = 30;

    private JudgePerformanceScorePolicy() {
    }

    static int minimumLength(String role, Integer configured) {
        if (configured != null && configured > 0) {
            return configured;
        }
        return "CROSS".equals(role) ? DEFAULT_CROSS_MIN : DEFAULT_OTHER_MIN;
    }

    static double normalizedCommentLength(int chars, int minimumLength) {
        int effectiveChars = Math.max(chars, 0);
        return minimumLength <= 0 ? effectiveChars : (double) effectiveChars / minimumLength;
    }

    static double averageRequirementRatio(double totalRequirementRatio, int recordCount) {
        return recordCount <= 0 ? 0 : totalRequirementRatio / recordCount;
    }

    static double percentile(int participantCount, long greaterCount) {
        return participantCount <= 1
                ? 1
                : (double) (participantCount - 1 - greaterCount) / (participantCount - 1);
    }

    static double commentScore(double percentile, boolean completed) {
        // 按评语字数百分位划分五档：前 20% 为 50 分，之后每档递减 10 分，后 20% 为 10 分。
        if (!completed) {
            return 0;
        }
        double normalizedPercentile = Math.max(0, Math.min(1, percentile));
        if (normalizedPercentile >= 0.8) {
            return COMMENT_SCORE_MAX;
        }
        if (normalizedPercentile >= 0.6) {
            return COMMENT_SCORE_MAX - COMMENT_SCORE_STEP;
        }
        if (normalizedPercentile >= 0.4) {
            return COMMENT_SCORE_MAX - COMMENT_SCORE_STEP * 2;
        }
        if (normalizedPercentile > 0.2) {
            return COMMENT_SCORE_MAX - COMMENT_SCORE_STEP * 3;
        }
        return COMMENT_SCORE_MIN;
    }

    static int topPercent(double percentile) {
        if (percentile <= 0) {
            return 100;
        }
        return Math.max(1, (int) Math.ceil((1 - percentile) * 100));
    }

}
