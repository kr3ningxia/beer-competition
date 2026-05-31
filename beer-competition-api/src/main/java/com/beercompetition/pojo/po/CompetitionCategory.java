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
@TableName("competition_category")
public class CompetitionCategory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private String name;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
