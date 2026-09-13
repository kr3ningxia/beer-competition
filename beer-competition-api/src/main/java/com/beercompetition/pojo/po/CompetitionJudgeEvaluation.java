package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("competition_judge_evaluation")
public class CompetitionJudgeEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long judgeAccountId;
    private Integer judgmentLevel;
    private Integer feedbackQualityLevel;
    private Integer ruleExecutionLevel;
    private Integer professionalismLevel;
    private BigDecimal manualScore;
    private Integer commentTotalChars;
    private Integer commentAverageChars;
    private Integer commentRecordCount;
    private BigDecimal commentRequirementRatio;
    private BigDecimal commentPercentile;
    private BigDecimal commentScore;
    private Integer taskCompletedCount;
    private Integer taskTotalCount;
    private BigDecimal completionRate;
    private BigDecimal totalScore;
    private Integer excellentCandidate;
    private String evidence;
    private String status;
    private Long evaluatedBy;
    private LocalDateTime evaluatedTime;
    private Long confirmedBy;
    private LocalDateTime confirmedTime;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
