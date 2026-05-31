package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.JudgeRoleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScoreConfigItemRequest {

    @NotNull(message = "评审角色不能为空")
    private JudgeRoleType judgeRoleType;

    @Valid
    @NotEmpty(message = "评分维度不能为空")
    private List<DimensionRequest> dimensions;
}
