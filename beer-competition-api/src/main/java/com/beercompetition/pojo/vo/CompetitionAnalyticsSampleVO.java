package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionAnalyticsSampleVO {

    private String title;
    private String meta;
    private String detail;
    private String tone;
}
