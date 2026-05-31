package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ConfigNameBatchUpdateRequest {

    @Valid
    @NotEmpty(message = "配置项不能为空")
    private List<ConfigNameItemRequest> items;
}
