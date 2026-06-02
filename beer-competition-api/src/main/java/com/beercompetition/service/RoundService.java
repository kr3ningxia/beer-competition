package com.beercompetition.service;

import com.beercompetition.pojo.dto.FirstRoundCreateRequest;
import com.beercompetition.pojo.dto.NextRoundCreateRequest;
import com.beercompetition.pojo.dto.RankingSubmitRequest;
import com.beercompetition.pojo.dto.RoundAllocationRequest;
import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.pojo.vo.JudgeRoundTableVO;
import com.beercompetition.pojo.vo.JudgeTaskVO;
import com.beercompetition.pojo.vo.ResultDraftVO;

import java.util.List;

public interface RoundService {

    List<CompetitionRoundVO> listCompetitionRounds(Long competitionId);

    List<CompetitionEntryVO> listEntryPool(Long competitionId);

    List<ResultDraftVO> buildResultDrafts(Long competitionId);

    void createFirstRound(Long competitionId, FirstRoundCreateRequest request);

    void saveRoundAllocation(Long competitionId, Long roundId, RoundAllocationRequest request);

    void publishRound(Long competitionId, Long roundId);

    void completeFirstRound(Long competitionId, Long roundId);

    void createNextRound(Long competitionId, NextRoundCreateRequest request);

    void lockRound(Long competitionId, Long roundId);

    void publishResults(Long competitionId);

    List<JudgeTaskVO> listMyTasks();

    JudgeRoundTableVO getMyRoundTable(Long roundTableId);

    void submitRanking(Long roundTableId, RankingSubmitRequest request);
}
