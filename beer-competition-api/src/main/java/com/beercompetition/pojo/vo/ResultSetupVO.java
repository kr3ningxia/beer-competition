package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultSetupVO {

    private Boolean awardsReady;
    private Boolean published;
    private Boolean championReady;
}
