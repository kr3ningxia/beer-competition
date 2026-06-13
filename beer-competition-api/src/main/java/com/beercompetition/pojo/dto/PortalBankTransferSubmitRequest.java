package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PortalBankTransferSubmitRequest {

    @NotEmpty(message = "请选择需要付款的酒款")
    @Size(max = 50, message = "单次转账最多选择 50 款酒")
    private List<@NotNull(message = "酒款不能为空") Long> entryIds;

    @NotNull(message = "请填写转账金额")
    @DecimalMin(value = "0.01", message = "转账金额必须大于 0")
    private BigDecimal amount;

    @Size(max = 128, message = "付款户名最多 128 个字")
    private String payerName;

    @NotNull(message = "请选择转账时间")
    private LocalDateTime transferTime;

    @NotBlank(message = "请填写转账备注")
    @Size(max = 255, message = "转账备注最多 255 个字")
    private String remark;

    private Long voucherAssetId;
}
