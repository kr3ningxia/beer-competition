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
@TableName("beer_coin_lot")
public class BeerCoinLot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String lotNo;
    private Long enterpriseAccountId;
    private Long purchaseOrderId;
    private Long totalQuantity;
    private Long remainingQuantity;
    private LocalDateTime availableFrom;
    private LocalDateTime expiresAt;
    private String sourceType;
    private String sourceOrderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
