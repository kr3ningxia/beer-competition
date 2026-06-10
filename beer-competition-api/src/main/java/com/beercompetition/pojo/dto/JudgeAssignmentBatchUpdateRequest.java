package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class JudgeAssignmentBatchUpdateRequest {

    @Valid
    private List<JudgeAssignmentItemRequest> items;
}
