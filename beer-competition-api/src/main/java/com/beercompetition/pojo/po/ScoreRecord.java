package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("score_record")
public class ScoreRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long roundId;
    private Long roundTableId;
    private Long beerEntryId;
    private Long judgeAccountId;
    private Long assignmentId;
    private String judgeRoleType;
    private String dimensionsJson;
    private BigDecimal totalScore;
    private String comments;
    @TableField("is_final")
    private Integer finalFlag;
    @TableField("is_advanced")
    private Integer advancedFlag;
    private BigDecimal consensusScore;
    private Integer durationSeconds;
    private Integer commentCharCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
