package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiveBoardMetricVO {

    private String key;
    private String label;
    private String value;
    private String unit;
    private String tone;
}
