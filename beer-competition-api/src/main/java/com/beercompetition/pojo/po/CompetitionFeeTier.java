package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("competition_fee_tier")
public class CompetitionFeeTier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Integer startQuantity;
    private BigDecimal discountRate;
    private Integer sortOrder;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
