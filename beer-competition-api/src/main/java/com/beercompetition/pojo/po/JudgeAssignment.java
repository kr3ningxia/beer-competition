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
@TableName("judge_assignment")
public class JudgeAssignment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long judgeAccountId;
    private Long tableId;
    private String role;
    private LocalDateTime createTime;
}
