package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PortalEntryLabelVO {

    private Long id;
    private String uuid;
    private String shortCode;
    private String name;
    private String competitionName;
    private String competitionCode;
    private String categoryName;
    private String style;
    private BigDecimal abv;
}
