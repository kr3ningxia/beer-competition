package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionStageCheckVO {

    private String key;
    private String label;
    private String state;
    private String group;
    private String message;
    private String targetTab;
}
