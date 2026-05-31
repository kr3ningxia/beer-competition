package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.JudgeRoleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class JudgeScoreSaveRequest {

    @NotBlank(message = "酒款 UUID 不能为空")
    private String beerUuid;

    @NotNull(message = "评审角色不能为空")
    private JudgeRoleType judgeRoleType;

    @Valid
    @NotEmpty(message = "评分维度不能为空")
    private List<DimensionRequest> dimensions;

    @NotNull(message = "总分不能为空")
    private BigDecimal totalScore;

    @NotBlank(message = "评语不能为空")
    private String comments;
}
