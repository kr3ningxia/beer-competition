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
@TableName("bank_transfer_payment_entry")
public class BankTransferPaymentEntry {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bankTransferPaymentId;
    private Long beerEntryId;
    private Long entryPaymentId;
    private BigDecimal amount;
    private LocalDateTime createTime;
}
