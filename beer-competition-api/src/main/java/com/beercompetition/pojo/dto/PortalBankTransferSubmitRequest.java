package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PortalBankTransferSubmitRequest {

    @NotNull(message = "请先选择一款要付款的酒")
    private Long entryId;

    @Size(max = 128, message = "付款户名最多 128 个字")
    private String payerName;

    @NotNull(message = "请选择转账时间")
    private LocalDateTime transferTime;

    @NotBlank(message = "请填写转账备注")
    @Size(max = 255, message = "转账备注最多 255 个字")
    private String remark;

    private Long voucherAssetId;
}
