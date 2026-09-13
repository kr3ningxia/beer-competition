package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserCreateRequest {

    @NotBlank(message = "请输入登录账号")
    @Size(max = 64, message = "登录账号不能超过64个字符")
    @Pattern(regexp = "^[A-Za-z0-9_\\-.]+$", message = "登录账号仅支持字母、数字、下划线、短横线和点")
    private String username;

    @NotBlank(message = "请输入管理员姓名")
    @Size(max = 64, message = "管理员姓名不能超过64个字符")
    private String name;

    @NotBlank(message = "请输入初始密码")
    @Size(min = 6, max = 32, message = "密码长度需为6到32位")
    private String password;

    /** 可选范围由创建者身份决定：平台账号创建可选平台超级管理员/平台赛事管理员，主办方账号创建可选主管理员/子管理员。 */
    @Pattern(regexp = "^(PLATFORM_SUPER_ADMIN|PLATFORM_EVENT_ADMIN|ORGANIZER_ADMIN|ORGANIZER_SUB_ADMIN)?$",
            message = "管理员类型不正确")
    private String adminType;
}
