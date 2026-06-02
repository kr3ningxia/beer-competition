package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoundTableAllocationRequest {

    private Long id;

    @NotBlank(message = "轮次桌名称不能为空")
    private String name;

    @NotBlank(message = "桌长不能为空")
    private String captainPublicId;

    @NotNull(message = "目标数量不能为空")
    @Min(value = 1, message = "目标数量必须大于 0")
    private Integer targetCount;

    private String targetMode;

    private Integer sortOrder;

    private List<String> entryUuids;
}
