package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserUpdateRequest {

    @NotBlank(message = "请输入管理员姓名")
    @Size(max = 64, message = "管理员姓名不能超过64个字符")
    private String name;

    /** 留空表示不修改类型；只能在同一侧内调整，且不能修改自己的类型。 */
    @Pattern(regexp = "^(PLATFORM_SUPER_ADMIN|PLATFORM_EVENT_ADMIN|ORGANIZER_ADMIN|ORGANIZER_SUB_ADMIN)?$",
            message = "管理员类型不正确")
    private String adminType;
}
