package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EntrySummaryVO {

    private Long id;
    private String uuid;
    private String name;
    private String competitionName;
    private String categoryName;
    private String status;
    private BigDecimal abv;
    private Integer total;
    private Integer pendingPayment;
    private Integer registered;
    private Integer stored;
    private Integer canceled;
    private Integer resultPublished;
}
