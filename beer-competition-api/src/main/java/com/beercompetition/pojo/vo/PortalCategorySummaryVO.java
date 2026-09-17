package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalCategorySummaryVO {

    private Long id;
    private String name;
}
