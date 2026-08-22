package com.beercompetition.competition;

/**
 * 评委文字评价的后台修订状态规则。
 */
public final class FeedbackCommentEditPolicy {

    private FeedbackCommentEditPolicy() {
    }

    /**
     * 结果发布后仍允许修订评价文字，归档后作为历史记录锁定。
     */
    public static boolean isEditable(String status) {
        return status != null && !"ARCHIVED".equals(status);
    }
}
