package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.RoundTableConfirmation;
import org.apache.ibatis.annotations.Insert;

public interface RoundTableConfirmationMapper extends BaseMapper<RoundTableConfirmation> {

    @Insert("""
            INSERT INTO round_table_confirmation
                (round_table_id, category_id, judge_account_id, result_version, status, confirmed_time)
            VALUES
                (#{roundTableId}, #{categoryId}, #{judgeAccountId}, #{resultVersion}, #{status}, #{confirmedTime})
            ON DUPLICATE KEY UPDATE
                status = VALUES(status),
                confirmed_time = VALUES(confirmed_time)
            """)
    int upsert(RoundTableConfirmation confirmation);
}
