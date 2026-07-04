package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiveBoardSummaryVO {

    private String eyebrow;
    private Integer done;
    private Integer total;
    private Integer percent;
    private String label;
}
