package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserUpdateRequest {

    @NotBlank(message = "请输入管理员姓名")
    @Size(max = 64, message = "管理员姓名不能超过64个字符")
    private String name;
}
