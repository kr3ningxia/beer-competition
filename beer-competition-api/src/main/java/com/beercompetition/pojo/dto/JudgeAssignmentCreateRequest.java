package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.JudgeRoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JudgeAssignmentCreateRequest {

    @NotNull(message = "比赛不能为空")
    private Long competitionId;

    @NotBlank(message = "评审不能为空")
    private String judgePublicId;

    @NotNull(message = "桌次不能为空")
    private Long tableId;

    @NotNull(message = "角色不能为空")
    private JudgeRoleType role;
}
