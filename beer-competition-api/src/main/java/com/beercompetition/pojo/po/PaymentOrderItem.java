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
@TableName("payment_order_item")
public class PaymentOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paymentOrderId;
    private Long beerEntryId;
    private Long entryPaymentId;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
