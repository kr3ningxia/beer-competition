package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class JudgeEntryVO {

    private Long id;
    private String uuid;
    private Long competitionId;
    private String categoryName;
    private String style;
    private BigDecimal abv;
    private String description;
    private List<EntryExtraFieldVO> extraFields;
}
