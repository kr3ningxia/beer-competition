package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

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

    private Boolean breweryConflictFlag = false;

    @Size(max = 500, message = "酒厂利益关系说明最多 500 个字符")
    private String breweryConflictText;

    @AssertTrue(message = "请填写相关酒厂或品牌名称及关系说明")
    public boolean isBreweryConflictTextValid() {
        return !Boolean.TRUE.equals(breweryConflictFlag) || StringUtils.hasText(breweryConflictText);
    }
}
