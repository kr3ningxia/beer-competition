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
@TableName("bank_transfer_payment")
public class BankTransferPayment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String transferNo;
    private Long breweryId;
    private Long portalAccountId;
    private Long competitionId;
    private Long beerEntryId;
    private Long entryPaymentId;
    private BigDecimal amount;
    private String payerName;
    private LocalDateTime transferTime;
    private String remark;
    private Long voucherAssetId;
    private String status;
    private Long adminId;
    private String adminNote;
    private LocalDateTime submittedTime;
    private LocalDateTime processedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
