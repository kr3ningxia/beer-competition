package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BeerCoinPricingVO {

    private Long id;
    private String productCode;
    private String name;
    private String status;
    private LocalDateTime effectiveTime;
    private Integer versionNo;
    private List<BeerCoinTierVO> tiers;
}
