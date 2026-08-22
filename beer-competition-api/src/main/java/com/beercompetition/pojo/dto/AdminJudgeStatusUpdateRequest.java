package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminJudgeStatusUpdateRequest {

    @NotNull(message = "评审状态不能为空")
    private Integer status;

    @Size(max = 200, message = "备注最多 200 个字符")
    private String reviewRemark;
}
