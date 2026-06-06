package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AdminFeedbackReviewEntryVO {

    private Long roundId;
    private Long roundTableId;
    private Long beerEntryId;
    private String beerUuid;
    private String entryName;
    private String labelCode;
    private String shortCode;
    private String roundName;
    private String tableName;
    private String categoryName;
    private String style;
    private Integer personalSubmitted;
    private Integer personalTotal;
    private Boolean captainSubmitted;
    private BigDecimal consensusScore;
    private Boolean advanced;
    private String status;
    private AdminFeedbackCaptainOpinionVO captainOpinion;
    private List<AdminFeedbackJudgeScoreVO> judges;
}
