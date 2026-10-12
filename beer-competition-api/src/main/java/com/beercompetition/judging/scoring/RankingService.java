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

    RankingConfirmationVO getRankingConfirmation(Long roundTableId, Long categoryId);

    RankingConfirmationVO confirmRankingRoundTable(Long roundTableId, RoundTableConfirmationRequest request);

    void finalizeRanking(Long roundTableId);

    void saveRankingDraft(Long roundTableId, RankingDraftSaveRequest request);

    /**
     * 评委调整（换人/改酒款）后重算本桌确认是否完成，满足则自动提交。
     */
    void refreshRankingTableReadiness(Long roundTableId);
}
