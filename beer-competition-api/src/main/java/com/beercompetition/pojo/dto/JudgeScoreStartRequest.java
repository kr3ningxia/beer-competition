package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.JudgeRoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JudgeScoreStartRequest {

    @NotBlank(message = "酒款 UUID 不能为空")
    private String beerUuid;

    @NotNull(message = "评审角色不能为空")
    private JudgeRoleType judgeRoleType;
}
