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
@TableName("competition_coin_settlement")
public class CompetitionCoinSettlement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long enterpriseAccountId;
    private String settlementType;
    private Integer effectiveEntryCount;
    private String billingTier;
    private Long requiredQuantity;
    private Long chargedQuantity;
    private String status;
    private Long ledgerId;
    private String idempotencyKey;
    private Long settledByAdminId;
    private LocalDateTime settledTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
