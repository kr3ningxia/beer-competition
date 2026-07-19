package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompetitionQuickSummaryVO {

    private ProgressSummaryVO progressSummary;
    private List<CompetitionAlertVO> alerts;
    private List<String> dataIntegrityIssues;
}
