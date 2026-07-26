package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EntryFieldItemRequest {

    private String fieldKey;

    private String fieldLabel;

    private String fieldType;

    @Size(max = 255, message = "提示文案不能超过 255 个字符")
    private String helpText;

    @Size(max = 100, message = "字段选项不能超过 100 个")
    private List<String> options;

    @NotNull(message = "必填标记不能为空")
    private Boolean required;

    @NotNull(message = "评审可见标记不能为空")
    private Boolean visibleToJudges;

    @Min(value = 0, message = "排序不能小于 0")
    private Integer sortOrder = 0;
}
