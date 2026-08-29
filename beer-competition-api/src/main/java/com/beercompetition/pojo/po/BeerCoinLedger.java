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
@TableName("beer_coin_ledger")
public class BeerCoinLedger {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ledgerNo;
    private Long enterpriseAccountId;
    private String direction;
    private Long quantity;
    private String businessType;
    private String businessId;
    private String idempotencyKey;
    private Long operatorAdminId;
    private String reason;
    private LocalDateTime createTime;
}
