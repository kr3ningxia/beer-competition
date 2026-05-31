package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.SmsBizType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SmsSendRequest {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotNull(message = "业务类型不能为空")
    private SmsBizType bizType;
}
