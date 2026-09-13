package com.beercompetition.service;

import com.beercompetition.pojo.dto.JudgePerformanceSaveRequest;
import com.beercompetition.pojo.vo.CompetitionJudgePerformanceSummaryVO;
import com.beercompetition.pojo.vo.JudgeAccountPerformanceVO;
import com.beercompetition.pojo.vo.JudgePerformanceVO;

import java.util.List;

/**
 * 管理比赛内评审表现评价及账号历史汇总。
 */
public interface JudgePerformanceService {

    CompetitionJudgePerformanceSummaryVO listCompetitionPerformances(Long competitionId);

    JudgePerformanceVO savePerformance(Long competitionId, String judgePublicId,
                                       JudgePerformanceSaveRequest request);

    List<JudgeAccountPerformanceVO> listAccountOverviews(List<String> judgePublicIds);

    JudgeAccountPerformanceVO getAccountHistory(String judgePublicId);
}
