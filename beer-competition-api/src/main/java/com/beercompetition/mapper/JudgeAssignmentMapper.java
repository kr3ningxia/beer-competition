package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.JudgeAssignment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface JudgeAssignmentMapper extends BaseMapper<JudgeAssignment> {

    @Select("""
            SELECT DISTINCT judge_account_id FROM (
              SELECT a.judge_account_id FROM competition_judge_assignment a JOIN competition c ON c.id = a.competition_id WHERE c.organizer_id = #{organizerId}
              UNION
              SELECT ra.judge_account_id FROM judge_recruitment_application ra JOIN judge_recruitment r ON r.id=ra.recruitment_id JOIN competition c2 ON c2.id=r.competition_id WHERE c2.organizer_id=#{organizerId} AND ra.status='ACCEPTED'
            ) x
            """)
    List<Long> selectJudgeIdsByOrganizer(@Param("organizerId") Long organizerId);
}
