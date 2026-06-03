package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortalEntryDeliverySubmitRequest {

    @NotBlank(message = "送样方式不能为空")
    private String deliveryMethod;

    @Size(max = 64, message = "快递公司不能超过64个字符")
    private String carrier;

    @Size(max = 64, message = "快递单号不能超过64个字符")
    private String trackingNo;

    @Size(max = 500, message = "送样备注不能超过500个字符")
    private String deliveryNote;
}
