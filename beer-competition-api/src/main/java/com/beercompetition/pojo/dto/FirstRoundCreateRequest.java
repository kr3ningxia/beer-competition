package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FirstRoundCreateRequest {

    private String allocationStrategy = "EVEN_SPLIT";

    @Min(value = 1, message = "每桌晋级数量必须大于 0")
    private Integer defaultTargetCount = 3;
}
