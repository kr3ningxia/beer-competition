package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ScoreConfirmationVO {

    private Long roundTableId;
    private String competitionType;
    private String tableName;
    private String status;
    private Integer resultVersion;
    private Integer confirmedCount;
    private Integer requiredCount;
    private Boolean mineConfirmed;
    private Boolean readyForConfirmation;
    private Boolean overrideFlag;
    private String overrideReason;
    private LocalDateTime overrideTime;
    private List<ScoreConfirmationEntryVO> entries;
}
