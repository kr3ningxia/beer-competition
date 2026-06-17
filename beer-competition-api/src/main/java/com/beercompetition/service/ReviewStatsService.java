package com.beercompetition.service;

import com.beercompetition.pojo.vo.ReviewStatsVO;

public interface ReviewStatsService {

    ReviewStatsVO getMyRoundTableStats(Long roundTableId);

    void evictReviewStats(Long roundId, Long roundTableId, Long judgeId, String judgeRoleType);
}
