package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ConfigNameItemRequest {

    @NotBlank(message = "名称不能为空")
    private String name;

    @Min(value = 0, message = "排序不能小于 0")
    private Integer sortOrder = 0;

    /**
     * 空值表示不限制，非空表示该组别只允许这些风格。
     */
    private List<String> styleNames;
}
