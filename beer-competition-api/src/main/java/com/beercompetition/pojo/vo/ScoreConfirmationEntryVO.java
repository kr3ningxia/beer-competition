package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ScoreConfirmationEntryVO {

    private Long beerEntryId;
    private String uuid;
    private String shortCode;
    private String categoryName;
    private String style;
    private BigDecimal consensusScore;
    private String comments;
    private Boolean advanced;
}
