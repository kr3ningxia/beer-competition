package com.beercompetition.service;

import com.beercompetition.pojo.dto.JudgeAssignmentCreateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentBatchUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgePhoneUpdateRequest;
import com.beercompetition.pojo.dto.JudgeProfileUpdateRequest;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.JudgeAccountVO;

import java.util.List;

public interface JudgeService {

    List<JudgeAccountVO> listJudges(Integer status, String keyword);

    JudgeAccountVO getJudgeDetail(String publicId);

    JudgeAccountVO getMyProfile();

    JudgeAccountVO updateMyProfile(JudgeProfileUpdateRequest request);

    JudgeAccountVO updateJudge(String publicId, AdminJudgeUpdateRequest request);

    JudgeAccountVO updateJudgePhone(String publicId, AdminJudgePhoneUpdateRequest request);

    JudgeAccountVO updateJudgeStatus(String publicId, AdminJudgeStatusUpdateRequest request);

    void createAssignment(JudgeAssignmentCreateRequest request);

    void updateCompetitionAssignments(Long competitionId, JudgeAssignmentBatchUpdateRequest request);

    List<CompetitionVO> listMyCompetitions();
}
