package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class JudgeAccountPerformanceVO {

    private String judgePublicId;
    private BigDecimal averageScore;
    private Integer evaluatedCompetitionCount;
    private Integer excellentCount;
    private BigDecimal latestScore;
    private String latestCompetitionName;
    private List<JudgePerformanceVO> history;
}
