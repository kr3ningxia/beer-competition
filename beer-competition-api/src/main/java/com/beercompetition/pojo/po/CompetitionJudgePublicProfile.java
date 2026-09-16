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
@TableName("competition_judge_public_profile")
public class CompetitionJudgePublicProfile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long judgeAccountId;
    private String name;
    private String qualification;
    private Long avatarAssetId;
    private String rolesJson;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
