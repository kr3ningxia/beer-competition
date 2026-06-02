package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JudgeProfileUpdateRequest {

    @Size(max = 64, message = "微信号最多 64 个字符")
    private String wechat;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名最多 64 个字符")
    private String name;

    @NotBlank(message = "资质信息不能为空")
    @Size(max = 255, message = "资质信息最多 255 个字符")
    private String qualification;
}
