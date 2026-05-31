package com.beercompetition.service;

import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;

import java.util.List;

public interface CompetitionService {

    List<CompetitionVO> listCompetitions();

    CompetitionVO createCompetition(CompetitionCreateRequest request);

    List<ScoreConfigVO> getScoreConfigs(Long competitionId);

    List<ScoreConfigVO> updateScoreConfigs(Long competitionId, ScoreConfigBatchUpdateRequest request);
}
