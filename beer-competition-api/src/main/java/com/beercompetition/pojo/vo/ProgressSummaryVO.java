package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProgressSummaryVO {

    private Integer finalized;
    private Integer total;
    private Integer advanced;
    private Integer commentWarnings;
}
