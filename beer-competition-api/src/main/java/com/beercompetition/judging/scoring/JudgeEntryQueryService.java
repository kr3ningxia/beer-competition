package com.beercompetition.judging.scoring;

import com.beercompetition.pojo.vo.JudgeEntryVO;

/**
 * 解析评委扫码目标，并按当前轮次和分桌关系返回匿名酒款信息。
 */
public interface JudgeEntryQueryService {

    JudgeEntryVO getJudgeEntry(String uuid);

    JudgeEntryVO resolveJudgeScan(String code);
}
