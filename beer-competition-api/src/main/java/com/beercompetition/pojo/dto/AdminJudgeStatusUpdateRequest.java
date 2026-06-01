package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminJudgeStatusUpdateRequest {

    @NotNull(message = "评审状态不能为空")
    private Integer status;

    @Size(max = 255, message = "审核备注最多 255 个字符")
    private String reviewRemark;
}
