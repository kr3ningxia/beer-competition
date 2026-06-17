package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("judge_score_session")
public class JudgeScoreSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long roundId;
    private Long roundTableId;
    private Long beerEntryId;
    private Long judgeAccountId;
    private String judgeRoleType;
    private LocalDateTime startedAt;
    private LocalDateTime firstSubmittedAt;
    private LocalDateTime lastSubmittedAt;
    private Integer durationSeconds;
    private Integer commentCharCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
