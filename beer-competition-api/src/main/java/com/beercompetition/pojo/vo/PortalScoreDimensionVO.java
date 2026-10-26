package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PortalScoreDimensionVO {

    private String key;
    private String label;
    private String kind;
    private Boolean scored;
    private Boolean checkbox;
    private String selectionMode;
    private BigDecimal score;
    private BigDecimal maxScore;
    private List<String> selectedOptions;
    private String note;
}
