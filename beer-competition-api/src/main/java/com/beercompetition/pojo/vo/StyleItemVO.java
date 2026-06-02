package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StyleItemVO {

    private Long id;
    private String categoryName;
    private String name;
    private String styleCode;
    private String description;
    private Integer status;
    private Integer sortOrder;
}
