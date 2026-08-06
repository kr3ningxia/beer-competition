package com.beercompetition.judging.round;

import com.beercompetition.pojo.vo.JudgeRoundTableVO;
import com.beercompetition.pojo.vo.JudgeTaskVO;

import java.util.List;

/**
 * 提供当前评委的轮次任务和评分桌提交能力。
 */
public interface JudgeRoundTaskService {

    List<JudgeTaskVO> listMyTasks();

    JudgeRoundTableVO getMyRoundTable(Long roundTableId);

    void submitScoreRoundTable(Long roundTableId);
}
