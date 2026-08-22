package com.beercompetition.competition;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackCommentEditPolicyTest {

    @Test
    void allowsPublishedCompetitionAndLocksArchivedCompetition() {
        assertThat(FeedbackCommentEditPolicy.isEditable("RESULT_CONFIRMING")).isTrue();
        assertThat(FeedbackCommentEditPolicy.isEditable("PUBLISHED")).isTrue();
        assertThat(FeedbackCommentEditPolicy.isEditable("ARCHIVED")).isFalse();
        assertThat(FeedbackCommentEditPolicy.isEditable(null)).isFalse();
    }
}
