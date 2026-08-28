package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortalOrganizerPaymentRequest {
    @Size(max = 255, message = "付款备注最多255个字")
    private String remark;
}
