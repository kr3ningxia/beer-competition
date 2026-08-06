package com.beercompetition.judging.assignment;

import com.beercompetition.pojo.dto.FirstRoundCreateRequest;
import com.beercompetition.pojo.dto.NextRoundCreateRequest;
import com.beercompetition.pojo.dto.RoundAllocationRequest;

/**
 * 创建轮次并保存酒款、评委和评审桌分配。
 */
public interface RoundAllocationService {

    void createFirstRound(Long competitionId, FirstRoundCreateRequest request);

    void saveRoundAllocation(Long competitionId, Long roundId, RoundAllocationRequest request);

    void syncRoundCandidates(Long competitionId, Long roundId);

    void deleteDraftRound(Long competitionId, Long roundId);

    void createNextRound(Long competitionId, NextRoundCreateRequest request);
}
