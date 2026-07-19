package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PortalPaymentOrderBankTransferRequest {

    @Size(max = 128, message = "付款账户名最多128个字")
    private String payerName;

    private LocalDateTime transferTime;

    @Size(max = 255, message = "转账备注最多255个字")
    private String remark;

    private Long voucherAssetId;
}
