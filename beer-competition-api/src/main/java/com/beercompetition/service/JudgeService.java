package com.beercompetition.service;

import com.beercompetition.pojo.dto.JudgeAssignmentCreateRequest;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.JudgeAccountVO;

import java.util.List;

public interface JudgeService {

    List<JudgeAccountVO> listJudges();

    void createAssignment(JudgeAssignmentCreateRequest request);

    List<CompetitionVO> listMyCompetitions();
}
