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
@TableName("round_result")
public class RoundResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long roundId;
    private Long roundTableId;
    private Long beerEntryId;
    private String resultType;
    private Integer rankNo;
    private String slotLabel;
    private Long submittedBy;
    private LocalDateTime submittedTime;
    private Integer lockedFlag;
}
