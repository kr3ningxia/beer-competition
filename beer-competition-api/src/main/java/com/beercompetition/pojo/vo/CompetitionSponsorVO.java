package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionSponsorVO {

    private Long id;
    private String tierLabel;
    private String sponsorName;
    private Long logoAssetId;
    private String logoUrl;
    private Integer sortOrder;
    private Boolean featured;
    private Boolean enabled;
}
