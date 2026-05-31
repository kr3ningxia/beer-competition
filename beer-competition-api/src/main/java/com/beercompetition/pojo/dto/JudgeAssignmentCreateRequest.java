package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.JudgeRoleType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JudgeAssignmentCreateRequest {

    @NotNull(message = "比赛不能为空")
    private Long competitionId;

    @NotNull(message = "评审不能为空")
    private Long judgeAccountId;

    @NotNull(message = "桌次不能为空")
    private Long tableId;

    @NotNull(message = "角色不能为空")
    private JudgeRoleType role;
}
