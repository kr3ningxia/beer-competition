package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class StyleLibraryUpsertRequest {

    @NotBlank(message = "风格库编码不能为空")
    private String code;

    @NotBlank(message = "风格库名称不能为空")
    private String name;

    @NotBlank(message = "版本不能为空")
    private String version;

    @NotBlank(message = "语言不能为空")
    private String language;

    @NotBlank(message = "来源不能为空")
    private String source;

    private Integer status = 1;
    private List<String> tags;

    @Valid
    private List<StyleCategoryRequest> categories;

    @Valid
    @NotEmpty(message = "风格条目不能为空")
    private List<StyleItemRequest> styles;
}
