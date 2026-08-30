package com.beercompetition.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.JudgeRecruitmentApplication;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
public interface JudgeRecruitmentApplicationMapper extends BaseMapper<JudgeRecruitmentApplication> {
 @Select("SELECT DISTINCT a.judge_account_id FROM judge_recruitment_application a JOIN judge_recruitment r ON r.id=a.recruitment_id JOIN competition c ON c.id=r.competition_id WHERE c.organizer_id=#{organizerId} AND a.status='ACCEPTED'")
 List<Long> selectAcceptedJudgeIdsByOrganizer(@Param("organizerId") Long organizerId);

 @Select("SELECT DISTINCT a.judge_account_id FROM judge_recruitment_application a JOIN judge_recruitment r ON r.id=a.recruitment_id WHERE r.competition_id=#{competitionId} AND a.status='ACCEPTED'")
 List<Long> selectAcceptedJudgeIdsByCompetition(@Param("competitionId") Long competitionId);

 @Select("SELECT a.id FROM judge_recruitment_application a JOIN judge_recruitment r ON r.id=a.recruitment_id WHERE r.competition_id=#{competitionId} AND a.judge_account_id=#{judgeAccountId} AND a.status='ACCEPTED' LIMIT 1")
 Long selectAcceptedApplicationId(@Param("competitionId") Long competitionId, @Param("judgeAccountId") Long judgeAccountId);
}
