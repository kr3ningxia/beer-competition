package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompetitionAnalyticsFeedbackVO {

    private Integer commentCount;
    private Integer entryCount;
    private List<CompetitionAnalyticsPhraseVO> wordCloud;
    private List<CompetitionAnalyticsPhraseVO> positivePhrases;
    private List<CompetitionAnalyticsPhraseVO> negativePhrases;
    private List<CompetitionAnalyticsSampleVO> samples;
}
