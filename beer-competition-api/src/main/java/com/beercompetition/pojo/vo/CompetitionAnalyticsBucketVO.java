package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CompetitionAnalyticsBucketVO {

    private String key;
    private String label;
    private Integer count;
    private BigDecimal amount;
    private BigDecimal share;
    private String tone;
    private String detail;
}
