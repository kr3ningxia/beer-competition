package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionAnalyticsPhraseVO {

    private String text;
    private Integer count;
    private Integer entryCount;
    private Integer judgeCount;
    private Integer weight;
    private String tone;
    private String sample;
}
