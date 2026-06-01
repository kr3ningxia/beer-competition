package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.JudgeRoleType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JudgeAssignmentItemRequest {

    @NotNull(message = "评审桌不能为空")
    private Long tableId;

    @NotNull(message = "评审不能为空")
    private Long judgeAccountId;

    @NotNull(message = "评审角色不能为空")
    private JudgeRoleType role;
}
