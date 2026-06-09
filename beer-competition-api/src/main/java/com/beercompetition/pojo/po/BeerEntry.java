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
@TableName("beer_entry")
public class BeerEntry {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private Long competitionId;
    private Long breweryId;
    private Long categoryId;
    private String name;
    private String style;
    private BigDecimal abv;
    private String extraFieldsJson;
    private String status;
    private Integer storedFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
