package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CompetitionSponsorBatchUpdateRequest {

    @NotNull(message = "赞助商列表不能为空")
    @Valid
    private List<CompetitionSponsorItemRequest> items;
}
