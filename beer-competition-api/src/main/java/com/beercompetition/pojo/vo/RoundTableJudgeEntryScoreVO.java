package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RoundTableJudgeEntryScoreVO {

    private String beerUuid;
    private Boolean scored;
    private BigDecimal totalScore;
    private LocalDateTime submittedAt;
}
