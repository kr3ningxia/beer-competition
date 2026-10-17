package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompetitionConfigNameVO {

    private Long id;
    private String name;
    private String categoryName;
    private String styleCode;
    private String description;
    private Integer sortOrder;

    /**
     * 空值表示全部风格，非空表示仅允许这些风格。
     */
    private List<String> styleNames;
}
