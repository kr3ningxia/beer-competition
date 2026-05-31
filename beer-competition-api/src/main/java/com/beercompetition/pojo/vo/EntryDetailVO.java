package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class EntryDetailVO {

    private Long id;
    private String uuid;
    private String name;
    private String style;
    private BigDecimal abv;
    private String description;
    private String categoryName;
    private String competitionName;
    private LocalDate competitionDate;
    private String status;
    private List<EntryExtraFieldVO> extraFields;
}
