package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CategoryStyleScopeItemRequest {

    @NotNull(message = "投递组别不能为空")
    private Long categoryId;

    /**
     * 空值表示不限制，非空表示只允许这些风格。
     */
    private List<String> styleNames;
}
