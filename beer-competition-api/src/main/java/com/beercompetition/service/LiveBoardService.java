package com.beercompetition.service;

import com.beercompetition.pojo.vo.CompetitionLiveBoardVO;

public interface LiveBoardService {

    CompetitionLiveBoardVO getCompetitionLiveBoard(Long competitionId);
}
