package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class JudgePerformanceVO {

    private Long competitionId;
    private String competitionName;
    private LocalDate competitionDate;
    private String competitionStatus;
    private String judgePublicId;
    private String judgeName;
    private String role;
    private String roleLabel;
    private String tableName;
    private Integer taskCompletedCount;
    private Integer taskTotalCount;
    private BigDecimal completionRate;
    private Integer commentTotalChars;
    private Integer commentAverageChars;
    private Integer commentRecordCount;
    private BigDecimal commentRequirementRatio;
    private BigDecimal commentPercentile;
    private Integer commentTopPercent;
    private BigDecimal commentScore;
    private Integer commentRank;
    private Integer participantCount;
    private Integer judgmentLevel;
    private Integer feedbackQualityLevel;
    private Integer ruleExecutionLevel;
    private Integer professionalismLevel;
    private BigDecimal manualScore;
    private BigDecimal totalScore;
    private Integer overallRank;
    private Boolean excellentCandidate;
    private Boolean sampleWarning;
    private String evidence;
    private String evaluationStatus;
    private String evaluatedByName;
    private LocalDateTime evaluatedTime;
    private LocalDateTime confirmedTime;
    private Integer version;
}
