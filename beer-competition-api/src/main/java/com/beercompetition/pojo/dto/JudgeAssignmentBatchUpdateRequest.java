package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class JudgeAssignmentBatchUpdateRequest {

    @Valid
    @NotEmpty(message = "评审分配不能为空")
    private List<JudgeAssignmentItemRequest> items;
}
