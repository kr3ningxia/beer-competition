package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.RoundJudgeRankingDraft;
import org.apache.ibatis.annotations.Insert;

public interface RoundJudgeRankingDraftMapper extends BaseMapper<RoundJudgeRankingDraft> {

    @Insert("""
            INSERT INTO round_judge_ranking_draft
                (competition_id, round_id, round_table_id, category_id, judge_account_id, rankings_json)
            VALUES
                (#{competitionId}, #{roundId}, #{roundTableId}, #{categoryId}, #{judgeAccountId}, #{rankingsJson})
            ON DUPLICATE KEY UPDATE
                rankings_json = VALUES(rankings_json),
                update_time = CURRENT_TIMESTAMP
            """)
    int upsert(RoundJudgeRankingDraft draft);
}
