package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeerCoinPricingRequest {

    @NotEmpty(message = "至少配置一档价格阶梯")
    @Valid
    private List<BeerCoinTierRequest> tiers;
}
