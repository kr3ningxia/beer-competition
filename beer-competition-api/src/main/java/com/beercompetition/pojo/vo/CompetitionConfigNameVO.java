package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionConfigNameVO {

    private Long id;
    private String name;
    private String categoryName;
    private String styleCode;
    private String description;
    private Integer sortOrder;
}
