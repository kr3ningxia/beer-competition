package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionPrimaryActionVO {

    private String text;
    private String targetTab;
    private Boolean enabled;
}
