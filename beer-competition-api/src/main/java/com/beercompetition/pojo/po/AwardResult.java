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
@TableName("award_result")
public class AwardResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long categoryId;
    private Long beerEntryId;
    private Long awardRuleId;
    private String awardType;
    private String awardName;
    private Integer rankNo;
    private Long sourceRoundId;
    private Long sourceRoundTableId;
    private Long sourceResultId;
    private Integer championFlag;
    private Long confirmedBy;
    private LocalDateTime confirmedTime;
    private LocalDateTime publishedTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
