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
@TableName("competition_round")
public class CompetitionRound {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Integer roundNo;
    private String roundName;
    private String roundType;
    private Long sourceRoundId;
    private String status;
    private Integer sortOrder;
    private LocalDateTime publishedTime;
    private LocalDateTime submittedTime;
    private LocalDateTime lockedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
