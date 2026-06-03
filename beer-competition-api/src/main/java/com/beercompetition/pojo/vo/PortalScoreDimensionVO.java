package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PortalScoreDimensionVO {

    private String key;
    private String label;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String note;
}
