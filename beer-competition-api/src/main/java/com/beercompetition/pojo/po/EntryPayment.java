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
@TableName("entry_payment")
public class EntryPayment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long beerEntryId;
    private Long paymentOrderId;
    private BigDecimal amount;
    private String status;
    private String payMethod;
    private String outTradeNo;
    private String wechatTransactionId;
    private Long bankTransferId;
    private String codeUrl;
    private LocalDateTime expireTime;
    private BigDecimal paidAmount;
    private String wechatTradeState;
    private String wechatTradeStateDesc;
    private String notifyRawJson;
    private LocalDateTime lastQueryTime;
    private LocalDateTime paidTime;
    private Long confirmedByAdminId;
    private String confirmRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
