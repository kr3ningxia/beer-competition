package com.beercompetition.service;

import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.JudgeScoreStartRequest;
import com.beercompetition.pojo.dto.JudgeScoreUpdateRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.pojo.vo.ScoreRecordVO;

import java.util.List;

public interface ScoreService {

    ScoreConfigVO getCurrentScoreConfig(JudgeRoleType role, Long competitionId);

    void startScore(JudgeScoreStartRequest request);

    ScoreRecordVO createScore(JudgeScoreSaveRequest request);

    ScoreRecordVO updateScore(Long scoreId, JudgeScoreUpdateRequest request);

    List<ScoreRecordVO> listMyScores();

    ScoreRecordVO getMyScore(String uuid);

    List<ScoreRecordVO> listTableScores(String uuid);

    ScoreRecordVO finalizeTableScore(String uuid, TableScoreFinalizeRequest request);
}
