package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserPasswordResetRequest {

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "密码长度需为6到32位")
    private String password;
}
