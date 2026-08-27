package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 主办方入驻申请资料。
 */
@Data
public class OrganizerApplicationSubmitRequest {

    @NotBlank(message = "主体名称不能为空")
    @Size(max = 128, message = "主体名称不能超过128个字符")
    private String organizationName;

    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 64, message = "联系人姓名不能超过64个字符")
    private String contactName;

    @NotBlank(message = "联系人手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "联系人手机号格式不正确")
    private String contactPhone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过128个字符")
    private String contactEmail;

    @Size(max = 64, message = "微信号不能超过64个字符")
    private String wechat;

    @Size(max = 1000, message = "办赛简介不能超过1000个字符")
    private String businessDescription;

    @Size(max = 255, message = "预计赛事规模不能超过255个字符")
    private String expectedScale;

    @Size(max = 1000, message = "补充说明不能超过1000个字符")
    private String supplementalNote;

    private Long materialAssetId;
}
