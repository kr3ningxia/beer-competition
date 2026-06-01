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
@TableName("competition_score_config")
public class CompetitionScoreConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private String judgeRoleType;
    private String dimensionsJson;
    private Integer minCommentLength;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
