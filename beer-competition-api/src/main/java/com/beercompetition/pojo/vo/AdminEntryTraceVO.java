package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminEntryTraceVO {

    private String type;
    private Long roundId;
    private String roundName;
    private Long roundTableId;
    private String tableName;
    private String status;
    private String resultType;
    private Integer rankNo;
    private String slotLabel;
    private Boolean advanced;
    private String awardName;
    private String awardType;
}
