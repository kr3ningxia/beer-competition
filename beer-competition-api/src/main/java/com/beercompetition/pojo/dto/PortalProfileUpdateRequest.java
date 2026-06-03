package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortalProfileUpdateRequest {

    @NotBlank(message = "账号名称不能为空")
    @Size(max = 64, message = "账号名称不能超过64个字符")
    private String displayName;

    @NotBlank(message = "厂牌名称不能为空")
    @Size(max = 128, message = "厂牌名称不能超过128个字符")
    private String companyName;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 64, message = "联系人不能超过64个字符")
    private String contactName;

    @Size(max = 64, message = "微信号不能超过64个字符")
    private String wechat;
}
