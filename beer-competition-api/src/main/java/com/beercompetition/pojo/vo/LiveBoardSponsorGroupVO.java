package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LiveBoardSponsorGroupVO {

    private String tierLabel;
    private Boolean featured;
    private List<CompetitionSponsorVO> sponsors;
}
