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
    private String style;
    private String status;
    private Boolean published;
    private String lockReason;
    private PortalRoundResultVO roundResult;
}
