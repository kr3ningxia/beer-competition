package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminOfflineRefundRequest {

    @Size(max = 300, message = "处理说明不能超过300个字符")
    private String reason;
}
