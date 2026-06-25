package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("competition")
public class Competition {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private LocalDate competitionDate;
    private LocalDateTime registrationStart;
    private LocalDateTime registrationDeadline;
    private String status;
    private String competitionType;
    private BigDecimal entryFee;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal earlyBirdFee;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime earlyBirdDeadline;
    private String description;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String rulesUrl;
    private String styleLibraryVersion;
    private String deliveryMethod;
    private LocalDateTime sampleArrivalStart;
    private LocalDateTime sampleArrivalDeadline;
    private String sampleQuantityNote;
    private String deliveryRecipient;
    private String deliveryPhone;
    private String deliveryAddress;
    private String deliveryNote;
    private String logisticsVisibility;
    @TableLogic(value = "0", delval = "1")
    private Integer deletedFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
