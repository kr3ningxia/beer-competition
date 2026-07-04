package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CompetitionLiveBoardVO {

    private Long competitionId;
    private String competitionName;
    private String subtitle;
    private Long roundId;
    private String roundName;
    private String roundType;
    private String roundTypeText;
    private String roundStatus;
    private String statusText;
    private LocalDateTime refreshedAt;
    private LiveBoardSummaryVO summary;
    private List<LiveBoardMetricVO> metrics;
    private List<LiveBoardTableVO> tables;
    private List<LiveBoardSponsorGroupVO> sponsorGroups;
}
