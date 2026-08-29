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
@TableName("beer_coin_product")
public class BeerCoinProduct {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String productCode;
    private String name;
    private String status;
    private LocalDateTime effectiveTime;
    private Integer versionNo;
    private Long createdByAdminId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
