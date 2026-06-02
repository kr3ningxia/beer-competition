package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.Competition;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionMapper extends BaseMapper<Competition> {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM `competition`
            WHERE `code` = #{code}
            <if test="currentId != null">
              AND `id` != #{currentId}
            </if>
            </script>
            """)
    Long countByCodeIncludingDeleted(@Param("code") String code, @Param("currentId") Long currentId);
}
