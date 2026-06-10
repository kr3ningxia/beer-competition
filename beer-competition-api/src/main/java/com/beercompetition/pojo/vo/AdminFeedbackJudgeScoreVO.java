package com.beercompetition.pojo.vo;

import com.beercompetition.pojo.dto.DimensionRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminFeedbackJudgeScoreVO {

    private Long scoreRecordId;
    private String judgePublicId;
    private String judgeName;
    private String role;
    private String roleLabel;
    private Boolean scored;
    private Boolean editable;
    private BigDecimal totalScore;
    private BigDecimal maxTotal;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private List<DimensionRequest> dimensions;
    private String comments;
    private String anomaly;
}
