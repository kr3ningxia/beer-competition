package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CategoryStyleScopeBatchUpdateRequest {

    @Valid
    @NotEmpty(message = "投递组别不能为空")
    private List<CategoryStyleScopeItemRequest> items;
}
