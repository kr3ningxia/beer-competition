package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompetitionJudgePerformanceSummaryVO {

    private Long competitionId;
    private String competitionName;
    private Boolean draftAllowed;
    private Boolean confirmAllowed;
    private String confirmDisabledReason;
    private Integer totalJudges;
    private Integer evaluatedCount;
    private Integer confirmedCount;
    private Integer excellentCandidateCount;
    private List<JudgePerformanceVO> records;
}
