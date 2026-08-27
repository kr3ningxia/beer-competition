package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.JudgeAssignment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface JudgeAssignmentMapper extends BaseMapper<JudgeAssignment> {

    @Select("""
            SELECT DISTINCT a.judge_account_id
              FROM competition_judge_assignment a
              JOIN competition c ON c.id = a.competition_id
             WHERE c.organizer_id = #{organizerId}
            """)
    List<Long> selectJudgeIdsByOrganizer(@Param("organizerId") Long organizerId);
}
