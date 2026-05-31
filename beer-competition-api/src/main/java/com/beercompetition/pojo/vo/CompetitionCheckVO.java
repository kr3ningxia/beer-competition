package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionCheckVO {

    private String key;
    private String label;
    private String state;
    private String message;
}
