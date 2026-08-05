package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.RoundResult;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface RoundResultMapper extends BaseMapper<RoundResult> {

    @Select("""
            SELECT COALESCE(ROUND(AVG(TIMESTAMPDIFF(MINUTE, #{publishedTime}, submitted_time))), 0)
            FROM round_result
            WHERE round_id = #{roundId}
              AND #{publishedTime} IS NOT NULL
              AND submitted_time >= #{publishedTime}
            """)
    Integer selectAverageReviewMinutes(@Param("roundId") Long roundId,
                                       @Param("publishedTime") LocalDateTime publishedTime);
}
