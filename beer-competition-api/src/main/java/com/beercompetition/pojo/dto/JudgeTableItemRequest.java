package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JudgeTableItemRequest {

    @NotBlank(message = "评审桌名称不能为空")
    private String tableName;
}
