package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class NextRoundCreateRequest {

    @NotNull(message = "来源轮次不能为空")
    private Long sourceRoundId;

    @NotBlank(message = "轮次名称不能为空")
    private String roundName;

    private String strategy = "MANUAL";

    @Min(value = 1, message = "桌数必须大于 0")
    private Integer tableCount = 1;

    private String targetMode = "TOP_N";

    @Min(value = 1, message = "目标数量必须大于 0")
    private Integer targetCount = 3;

    private List<String> captainPublicIds;
}
