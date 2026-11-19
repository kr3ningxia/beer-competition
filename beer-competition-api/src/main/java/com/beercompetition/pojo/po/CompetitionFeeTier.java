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
    /** 折后单价（元/款），阶梯价以金额为准。 */
    private BigDecimal tierPrice;
    /** 由折后单价换算出的折扣率，用于按早鸟价折上折计价。 */
    private BigDecimal discountRate;
    private Integer sortOrder;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
