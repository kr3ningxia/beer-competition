package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompetitionRefundPolicyUpdateRequest {

    @NotBlank(message = "请选择退款审批方式")
    private String refundApprovalMode;
}
