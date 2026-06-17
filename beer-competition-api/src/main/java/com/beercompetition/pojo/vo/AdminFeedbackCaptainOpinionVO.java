package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminFeedbackCaptainOpinionVO {

    private Long scoreRecordId;
    private Boolean submitted;
    private Boolean editable;
    private String captainName;
    private BigDecimal consensusScore;
    private BigDecimal maxConsensus;
    private Boolean advanced;
    private String comments;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private Integer commentCharCount;
    private Integer minCommentLength;
}
