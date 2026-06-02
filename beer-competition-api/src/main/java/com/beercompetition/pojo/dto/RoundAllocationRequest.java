package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RoundAllocationRequest {

    @Valid
    @NotEmpty(message = "轮次桌不能为空")
    private List<RoundTableAllocationRequest> tables;
}
