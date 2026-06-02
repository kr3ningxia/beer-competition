package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultDraftVO {

    private String category;
    private String slot;
    private String uuid;
    private Long beerEntryId;
}
