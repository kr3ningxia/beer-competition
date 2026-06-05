package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JudgeRoundTableVO {

    private Long roundTableId;
    private Long roundId;
    private String roundName;
    private String roundType;
    private String tableName;
    private Integer targetCount;
    private Integer expectedJudgeCount;
    private String targetMode;
    private String status;
    private Boolean canSubmitRanking;
    private List<CompetitionEntryVO> entries;
    private List<RoundRankingSlotVO> rankings;
}
