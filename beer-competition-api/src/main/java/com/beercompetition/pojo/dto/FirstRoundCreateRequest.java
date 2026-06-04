package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class FirstRoundCreateRequest {

    private String allocationStrategy = "EVEN_SPLIT";

    @Min(value = 1, message = "每桌晋级数量必须大于 0")
    private Integer defaultTargetCount = 3;

    @Valid
    private List<RoundTableAllocationRequest> tables;
}
