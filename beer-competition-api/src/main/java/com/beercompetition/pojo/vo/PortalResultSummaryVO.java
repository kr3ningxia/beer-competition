package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalResultSummaryVO {

    private Long entryId;
    private String entryName;
    private Long competitionId;
    private String competitionName;
    private String categoryName;
    private Integer categoryEntryCount;
    private String style;
    private String status;
    private Boolean published;
    private String lockReason;
    private String awardName;
    private String awardType;
    private Boolean champion;
    private PortalRoundResultVO roundResult;
}
