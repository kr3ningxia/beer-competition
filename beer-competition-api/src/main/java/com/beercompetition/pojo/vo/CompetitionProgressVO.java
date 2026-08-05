package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompetitionProgressVO {

    private ProgressSummaryVO progressSummary;
    private List<CompetitionRoundVO> rounds;
    private CompetitionRoundVO currentRound;
}
