package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionConfigNameVO {

    private Long id;
    private String name;
    private Integer sortOrder;
}
