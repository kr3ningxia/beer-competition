package com.beercompetition.service.impl;

final class JudgePerformanceScorePolicy {

    private static final int COMMENT_SCORE_MAX = 30;
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
        return completed ? Math.min(COMMENT_SCORE_MAX, 12 + 20 * percentile) : 0;
    }

    static int topPercent(double percentile) {
        if (percentile <= 0) {
            return 100;
        }
        return Math.max(1, (int) Math.ceil((1 - percentile) * 100));
    }

    static double weightedLevel(int level, int weight) {
        double ratio = switch (level) {
            case 1 -> 0;
            case 2 -> 0.6;
            case 3 -> 0.75;
            case 4 -> 0.9;
            default -> 1;
        };
        return weight * ratio;
    }
}
