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
@TableName("registration_batch")
public class RegistrationBatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private Long competitionId;
    private Long breweryId;
    private Long portalAccountId;
    private Integer entryCount;
    private BigDecimal totalAmount;
    private String status;
    private String idempotencyKey;
    private Integer rulesAccepted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
