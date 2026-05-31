package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EntryFieldItemRequest {

    @NotBlank(message = "字段 key 不能为空")
    private String fieldKey;

    @NotBlank(message = "字段名称不能为空")
    private String fieldLabel;

    @NotBlank(message = "字段类型不能为空")
    private String fieldType;

    @NotNull(message = "必填标记不能为空")
    private Boolean required;

    @NotNull(message = "评审可见标记不能为空")
    private Boolean visibleToJudges;

    @Min(value = 0, message = "排序不能小于 0")
    private Integer sortOrder = 0;
}
