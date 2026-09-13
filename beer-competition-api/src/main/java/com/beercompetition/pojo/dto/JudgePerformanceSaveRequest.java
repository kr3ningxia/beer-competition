package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JudgePerformanceSaveRequest {

    @Min(value = 1, message = "专业判断等级不能小于 1")
    @Max(value = 5, message = "专业判断等级不能大于 5")
    private Integer judgmentLevel;

    @Min(value = 1, message = "反馈质量等级不能小于 1")
    @Max(value = 5, message = "反馈质量等级不能大于 5")
    private Integer feedbackQualityLevel;

    @Min(value = 1, message = "规则执行等级不能小于 1")
    @Max(value = 5, message = "规则执行等级不能大于 5")
    private Integer ruleExecutionLevel;

    @Min(value = 1, message = "职业表现等级不能小于 1")
    @Max(value = 5, message = "职业表现等级不能大于 5")
    private Integer professionalismLevel;

    @Size(max = 1000, message = "评价依据不能超过 1000 字")
    private String evidence;

    @NotBlank(message = "保存状态不能为空")
    @Pattern(regexp = "^(DRAFT|CONFIRMED)$", message = "保存状态不合法")
    private String status;

    @NotNull(message = "版本号不能为空")
    @Min(value = 0, message = "版本号不能小于 0")
    private Integer version;
}
