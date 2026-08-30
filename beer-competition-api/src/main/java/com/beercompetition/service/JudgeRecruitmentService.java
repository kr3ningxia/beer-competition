package com.beercompetition.service;

import com.beercompetition.pojo.dto.JudgeRecruitmentApplicationRequest;
import com.beercompetition.pojo.dto.JudgeRecruitmentRequest;
import com.beercompetition.pojo.dto.JudgeRecruitmentReviewRequest;
import com.beercompetition.pojo.vo.JudgeRecruitmentApplicationVO;
import com.beercompetition.pojo.vo.JudgeRecruitmentVO;
import java.util.List;

public interface JudgeRecruitmentService {
    List<JudgeRecruitmentVO> adminList(String status, String keyword);
    JudgeRecruitmentVO adminGet(Long id);
    JudgeRecruitmentVO save(JudgeRecruitmentRequest request, Long id);
    JudgeRecruitmentVO publish(Long id);
    JudgeRecruitmentVO close(Long id);
    JudgeRecruitmentVO reopen(Long id);
    List<JudgeRecruitmentApplicationVO> applications(Long id, String status, String keyword);
    JudgeRecruitmentApplicationVO review(Long applicationId, JudgeRecruitmentReviewRequest request);
    List<JudgeRecruitmentVO> publicList();
    JudgeRecruitmentVO publicGet(Long id);
    List<JudgeRecruitmentVO> myApplications();
    JudgeRecruitmentApplicationVO apply(Long id, JudgeRecruitmentApplicationRequest request);
    JudgeRecruitmentApplicationVO updateApplication(Long applicationId, JudgeRecruitmentApplicationRequest request);
    JudgeRecruitmentApplicationVO withdraw(Long applicationId);
}
