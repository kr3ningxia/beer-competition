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
@TableName("entry_refund")
public class EntryRefund {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long beerEntryId;
    private Long entryPaymentId;
    private String refundNo;
    private BigDecimal amount;
    private String status;
    private String reason;
    private Long requestedByPortalId;
    private LocalDateTime requestedTime;
    private Long processedByAdminId;
    private LocalDateTime processedTime;
    private LocalDateTime successTime;
    private String failReason;
    private String wechatRefundId;
    private String outRefundNo;
    private String notifyRawJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
