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
@TableName("round_table")
public class RoundTable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long roundId;
    private String tableName;
    private Long captainJudgeId;
    private Integer targetCount;
    private String targetMode;
    private String status;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
