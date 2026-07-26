package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EntryFieldBatchUpdateRequest {

    @Valid
    @NotNull(message = "报名字段列表不能为空")
    @Size(max = 50, message = "报名补充字段不能超过 50 个")
    private List<EntryFieldItemRequest> items;
}
