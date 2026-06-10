package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RankingConfirmationSlotVO {

    private Integer rank;
    private String label;
    private Long beerEntryId;
    private String uuid;
    private String shortCode;
    private String categoryName;
    private String style;
}
