package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StyleCategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    @Min(value = 0, message = "排序不能小于 0")
    private Integer sortOrder = 0;
}
