package com.beercompetition.service;

import com.beercompetition.pojo.dto.AdminFeedbackCommentUpdateRequest;

public interface AdminFeedbackService {

    void updateScoreRecordComments(Long competitionId, Long scoreRecordId, AdminFeedbackCommentUpdateRequest request);
}
