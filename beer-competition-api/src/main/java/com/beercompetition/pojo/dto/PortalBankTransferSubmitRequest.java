package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PortalBankTransferSubmitRequest {

    @NotNull(message = "请先选择一款要支付的酒")
    private Long entryId;

    @Size(max = 128, message = "付款账户名最多 128 个字")
    private String payerName;

    private LocalDateTime transferTime;

    @Size(max = 255, message = "转账备注最多 255 个字")
    private String remark;

    private Long voucherAssetId;
}
