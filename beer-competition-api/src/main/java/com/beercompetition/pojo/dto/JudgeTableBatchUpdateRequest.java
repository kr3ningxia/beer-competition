package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class JudgeTableBatchUpdateRequest {

    @Valid
    @NotEmpty(message = "评审桌不能为空")
    private List<JudgeTableItemRequest> items;
}
