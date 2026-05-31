package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EntryFieldBatchUpdateRequest {

    @Valid
    @NotEmpty(message = "报名字段不能为空")
    private List<EntryFieldItemRequest> items;
}
