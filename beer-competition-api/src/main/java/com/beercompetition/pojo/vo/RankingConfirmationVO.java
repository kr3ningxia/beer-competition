package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RankingConfirmationVO {

    private Long roundTableId;
    private String tableName;
    private String status;
    private String targetMode;
    private Integer resultVersion;
    private Integer confirmedCount;
    private Integer requiredCount;
    private Boolean mineConfirmed;
    private Boolean readyForConfirmation;
    private Boolean readyForFinalSubmit;
    private Boolean overrideFlag;
    private String overrideReason;
    private LocalDateTime overrideTime;
    private List<RankingConfirmationSlotVO> slots;
}
