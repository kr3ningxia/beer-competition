package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PortalMyParticipationVO {

    private PortalProfileVO profile;
    private List<EntrySummaryVO> entries;
    private List<PortalCompetitionVO> competitions;
}
