package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionEntryVO {

    private Long id;
    private String uuid;
    private String shortCode;
    private String categoryName;
    private String style;
    private String status;
    private Boolean stored;
    private Boolean advanced;
    private String sourceTable;
    private String sourceResult;
}
