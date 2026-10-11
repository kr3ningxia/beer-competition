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
@TableName("round_table_confirmation")
public class RoundTableConfirmation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roundTableId;
    /** 组别独立确认维度；旧单组别记录允许为空。 */
    private Long categoryId;
    private Long judgeAccountId;
    private Integer resultVersion;
    private String status;
    private LocalDateTime confirmedTime;
}
