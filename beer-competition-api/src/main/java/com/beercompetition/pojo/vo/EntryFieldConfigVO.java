package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EntryFieldConfigVO {

    private Long id;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private String helpText;
    private List<String> options;
    private Boolean required;
    private Boolean visibleToJudges;
    private Integer sortOrder;
}
