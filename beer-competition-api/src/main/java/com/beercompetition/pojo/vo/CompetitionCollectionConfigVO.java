package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompetitionCollectionConfigVO {

    private Long competitionId;
    private Boolean tenantCompetition;
    private List<String> enabledMethods;
    private Long wechatQrAssetId;
    private String wechatQrUrl;
    private String bankAccountName;
    private String bankAccountNo;
    private String bankName;
    private String collectionNote;
    private String paymentContact;
}
