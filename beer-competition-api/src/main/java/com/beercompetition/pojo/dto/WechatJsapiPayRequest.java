package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatJsapiPayRequest {

    @NotBlank(message = "请先完成微信授权")
    private String code;
}
