package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.vo.CompetitionEntryStatsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface BeerEntryMapper extends BaseMapper<BeerEntry> {

    @Select("""
            <script>
            SELECT competition_id,
                   COUNT(*) AS total_count,
                   SUM(CASE WHEN status = #{pendingPaymentStatus} THEN 1 ELSE 0 END) AS pending_payment_count,
                   SUM(CASE WHEN status != #{canceledStatus} THEN 1 ELSE 0 END) AS registered_count,
                   SUM(CASE WHEN stored_flag = #{storedFlag} THEN 1 ELSE 0 END) AS stored_count,
                   SUM(CASE WHEN status = #{canceledStatus} THEN 1 ELSE 0 END) AS canceled_count,
                   SUM(CASE WHEN status IN (#{resultPublishedStatus}, #{legacyPublishedStatus}) THEN 1 ELSE 0 END) AS result_published_count
            FROM beer_entry
            WHERE competition_id IN
            <foreach collection="competitionIds" item="competitionId" open="(" separator="," close=")">
              #{competitionId}
            </foreach>
            GROUP BY competition_id
            </script>
            """)
    List<CompetitionEntryStatsVO> selectCompetitionStats(
            @Param("competitionIds") Collection<Long> competitionIds,
            @Param("pendingPaymentStatus") String pendingPaymentStatus,
            @Param("canceledStatus") String canceledStatus,
            @Param("resultPublishedStatus") String resultPublishedStatus,
            @Param("legacyPublishedStatus") String legacyPublishedStatus,
            @Param("storedFlag") Integer storedFlag);
}
