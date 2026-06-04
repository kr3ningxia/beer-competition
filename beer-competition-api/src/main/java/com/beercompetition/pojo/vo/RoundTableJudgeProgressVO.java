package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoundTableJudgeProgressVO {

    private String judgePublicId;
    private String judgeName;
    private String role;
    private String roleLabel;
    private Integer submittedCount;
    private Integer totalCount;
    private Integer progress;
    private List<String> missingEntryUuids;
    private List<RoundTableJudgeEntryScoreVO> entryScores;
}
