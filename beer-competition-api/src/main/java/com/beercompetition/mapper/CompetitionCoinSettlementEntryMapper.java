package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.CompetitionCoinSettlementEntry;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionCoinSettlementEntryMapper extends BaseMapper<CompetitionCoinSettlementEntry> {

    /**
     * 统计当前有效酒款中尚未写入结算快照的数量。
     */
    @Select("""
            SELECT COUNT(*)
            FROM beer_entry entry
            WHERE entry.competition_id = #{competitionId}
              AND entry.deleted_flag = 0
              AND entry.status IN ('REGISTERED', 'STORED', 'RESULT_PUBLISHED')
              AND NOT EXISTS (
                SELECT 1
                FROM competition_coin_settlement_entry snapshot
                WHERE snapshot.settlement_id = #{settlementId}
                  AND snapshot.beer_entry_id = entry.id
              )
            """)
    long countMissingEffectiveEntries(@Param("competitionId") Long competitionId,
                                      @Param("settlementId") Long settlementId);

    @Select("""
            SELECT COUNT(*)
            FROM competition_coin_settlement_entry
            WHERE settlement_id = #{settlementId}
              AND beer_entry_id = #{beerEntryId}
            """)
    long countSettlementEntry(@Param("settlementId") Long settlementId,
                              @Param("beerEntryId") Long beerEntryId);
}
