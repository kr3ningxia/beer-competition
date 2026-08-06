package com.beercompetition.judging.scoring;

import com.beercompetition.pojo.dto.RankingDraftSaveRequest;
import com.beercompetition.pojo.dto.RankingSubmitRequest;
import com.beercompetition.pojo.dto.RoundTableConfirmationRequest;
import com.beercompetition.pojo.vo.RankingConfirmationVO;

/**
 * 维护排名草稿、提交、多人确认和最终锁定。
 */
public interface RankingService {

    void submitRanking(Long roundTableId, RankingSubmitRequest request);

    RankingConfirmationVO getRankingConfirmation(Long roundTableId);

    RankingConfirmationVO confirmRankingRoundTable(Long roundTableId, RoundTableConfirmationRequest request);

    void finalizeRanking(Long roundTableId);

    void saveRankingDraft(Long roundTableId, RankingDraftSaveRequest request);
}
