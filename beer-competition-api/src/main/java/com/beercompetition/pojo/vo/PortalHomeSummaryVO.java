package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PortalHomeSummaryVO {

    private PortalCompetitionSummaryVO activeCompetition;
    private List<PortalCompetitionSummaryVO> openCompetitions;
}
