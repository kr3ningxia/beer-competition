package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoundRankingSlotVO {

    private Integer rank;
    private String label;
    private String uuid;
    private Long beerEntryId;
}
