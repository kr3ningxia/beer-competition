package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminJudgeUpdateRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 64, message = "微信号最多 64 个字符")
    private String wechat;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名最多 64 个字符")
    private String name;

    @NotBlank(message = "资质信息不能为空")
    @Size(max = 255, message = "资质信息最多 255 个字符")
    private String qualification;
}
