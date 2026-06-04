package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwardRuleVO {

    private Long id;
    private Long competitionId;
    private Long categoryId;
    private String categoryName;
    private String awardType;
    private String awardName;
    private Integer rankNo;
    private Boolean enabled;
    private Integer sortOrder;
}
