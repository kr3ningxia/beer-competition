package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.EntryRefund;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface EntryRefundMapper extends BaseMapper<EntryRefund> {

    @Select("""
            <script>
            SELECT r.*
              FROM entry_refund r
              JOIN beer_entry e ON e.id = r.beer_entry_id
             WHERE 1 = 1
            <if test="status != null and status != ''">
              AND r.status = #{status}
            </if>
            <if test="competitionIds != null and competitionIds.size() &gt; 0">
              AND e.competition_id IN
              <foreach collection="competitionIds" item="competitionId" open="(" separator="," close=")">
                #{competitionId}
              </foreach>
            </if>
            <if test="competitionIds != null and competitionIds.size() == 0">
              AND e.competition_id = -1
            </if>
             ORDER BY r.requested_time DESC, r.id DESC
            </script>
            """)
    List<EntryRefund> selectAdminRefunds(@Param("status") String status,
                                         @Param("competitionIds") Collection<Long> competitionIds);
}
