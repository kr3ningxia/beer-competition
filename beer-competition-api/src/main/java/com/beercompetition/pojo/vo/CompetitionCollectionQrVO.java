package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompetitionCollectionQrVO {

    private Long fileAssetId;
    private String fileName;
    private String adminUrl;
}
