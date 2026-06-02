package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("round_table_member")
public class RoundTableMember {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roundTableId;
    private Long judgeAccountId;
    private String role;
    private Integer systemTaskRequired;
}
