package com.beercompetition.judging.scoring;

import com.beercompetition.pojo.dto.AdminConfirmationOverrideRequest;
import com.beercompetition.pojo.dto.RoundTableConfirmationRequest;
import com.beercompetition.pojo.vo.ScoreConfirmationVO;

/**
 * 维护评分桌确认版本和管理端异常覆盖。
 */
public interface ScoreConfirmationService {

    ScoreConfirmationVO getScoreConfirmation(Long roundTableId);

    ScoreConfirmationVO confirmScoreRoundTable(Long roundTableId, RoundTableConfirmationRequest request);

    void overrideScoreConfirmation(Long competitionId, Long roundTableId, AdminConfirmationOverrideRequest request);
}
