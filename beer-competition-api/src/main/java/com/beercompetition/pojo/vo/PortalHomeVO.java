package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PortalHomeVO {

    private PortalCompetitionVO activeCompetition;
    private List<PortalCompetitionVO> openCompetitions;
    private List<PortalCompetitionVO> competitions;
}
