package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminFeedbackCaptainOpinionVO {

    private Boolean submitted;
    private String captainName;
    private BigDecimal consensusScore;
    private BigDecimal maxConsensus;
    private Boolean advanced;
    private String comments;
    private LocalDateTime submittedAt;
}
