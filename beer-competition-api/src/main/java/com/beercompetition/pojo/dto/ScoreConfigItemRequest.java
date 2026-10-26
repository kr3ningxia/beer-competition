package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.JudgeRoleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScoreConfigItemRequest {

    /**
     * 0 表示通用评分表，大于 0 表示组别专属评分表。
     */
    @Min(value = 0, message = "评分表组别不能小于 0")
    private Long categoryId = 0L;

    @NotNull(message = "评审角色不能为空")
    private JudgeRoleType judgeRoleType;

    @Valid
    @NotEmpty(message = "评分维度不能为空")
    private List<DimensionRequest> dimensions;

    @Min(value = 0, message = "评语字数下限不能小于 0")
    private Integer minCommentLength = 0;
}
