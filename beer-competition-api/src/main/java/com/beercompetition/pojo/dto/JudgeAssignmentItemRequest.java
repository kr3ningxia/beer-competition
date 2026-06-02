package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.JudgeRoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JudgeAssignmentItemRequest {

    @NotNull(message = "评审桌不能为空")
    private Long tableId;

    @NotBlank(message = "评审不能为空")
    private String judgePublicId;

    @NotNull(message = "评审角色不能为空")
    private JudgeRoleType role;
}
