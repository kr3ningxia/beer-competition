package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 一张评审桌内一个投递组别的独立排序与确认状态。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("round_table_category_state")
public class RoundTableCategoryState {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roundTableId;
    private Long categoryId;
    private Integer resultVersion;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
