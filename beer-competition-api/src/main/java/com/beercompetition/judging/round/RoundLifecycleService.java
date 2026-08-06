package com.beercompetition.judging.round;

/**
 * 推进轮次发布、完成、锁定和赛事结果发布状态。
 */
public interface RoundLifecycleService {

    void publishRound(Long competitionId, Long roundId);

    void completeFirstRound(Long competitionId, Long roundId);

    void lockRound(Long competitionId, Long roundId);

    void publishResults(Long competitionId);
}
