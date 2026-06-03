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
    private String labelCode;
    private String shortCode;
    private String scanToken;
    private Long competitionId;
    private String competitionName;
    private Long roundId;
    private String roundName;
    private String roundType;
    private Long roundTableId;
    private String tableName;
    private String judgeRoleType;
    private String taskType;
    private String action;
    private Boolean scored;
    private Boolean locked;
    private String categoryName;
    private String style;
    private String styleCategoryName;
    private String styleCode;
    private String styleDescription;
    private BigDecimal abv;
    private String description;
    private List<EntryExtraFieldVO> extraFields;
}
