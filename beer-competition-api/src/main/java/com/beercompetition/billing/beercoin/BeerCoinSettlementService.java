package com.beercompetition.billing.beercoin;

/**
 * 完成租户赛事开放报名和进入评审准备时的啤酒币结算。
 *
 * <p>实现必须使用稳定业务键保证重复请求不重复扣减，并在扣减时锁定可用批次。
 * 平台自办赛事应由实现根据可信比赛归属直接跳过，调用方不自行拼装租户判断。</p>
 */
public interface BeerCoinSettlementService {

    /**
     * 在开放报名的状态事务中完成最低消费结算。
     *
     * @param competitionId 比赛 ID
     */
    void settleRegistrationOpening(Long competitionId);

    /**
     * 在进入评审准备的状态事务中按有效酒款口径完成最终补扣。
     *
     * @param competitionId 比赛 ID
     */
    void settleJudgingPreparation(Long competitionId);

    /**
     * 要求比赛已完成评审准备结算，可用于分组、标签和裁判任务等后续门槛。
     *
     * @param competitionId 比赛 ID
     */
    void requireJudgingSettlementCompleted(Long competitionId);
}
