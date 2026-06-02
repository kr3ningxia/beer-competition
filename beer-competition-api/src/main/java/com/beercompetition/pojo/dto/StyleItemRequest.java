package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StyleItemRequest {

    @NotBlank(message = "风格分类不能为空")
    private String categoryName;

    @NotBlank(message = "风格名称不能为空")
    private String name;

    private String styleCode;
    private String description;
    private Integer status = 1;

    @Min(value = 0, message = "排序不能小于 0")
    private Integer sortOrder = 0;
}
