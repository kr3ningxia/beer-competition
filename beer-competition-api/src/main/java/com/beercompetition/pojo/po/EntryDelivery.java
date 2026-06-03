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
@TableName("entry_delivery")
public class EntryDelivery {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long beerEntryId;
    private String deliveryMethod;
    private String carrier;
    private String trackingNo;
    private String deliveryNote;
    private String deliveryStatus;
    private LocalDateTime submittedTime;
    private LocalDateTime receivedTime;
    private Long receivedByAdminId;
    private String receiveRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
