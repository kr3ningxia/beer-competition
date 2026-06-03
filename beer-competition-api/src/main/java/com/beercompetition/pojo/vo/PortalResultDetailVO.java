package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PortalResultDetailVO {

    private PortalResultSummaryVO summary;
    private List<PortalScoreRecordVO> scores;
    private List<PortalRoundResultVO> roundResults;
}
