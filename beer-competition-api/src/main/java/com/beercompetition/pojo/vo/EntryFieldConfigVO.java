package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntryFieldConfigVO {

    private Long id;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private Boolean required;
    private Boolean visibleToJudges;
    private Integer sortOrder;
}
