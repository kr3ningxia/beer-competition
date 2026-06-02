package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JudgeTaskVO {

    private String taskType;
    private Long competitionId;
    private String competitionName;
    private Long roundId;
    private String roundName;
    private Long roundTableId;
    private String tableName;
    private String judgeRoleType;
    private String roleLabel;
    private Integer totalEntries;
    private Integer completedCount;
}
