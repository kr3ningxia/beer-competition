package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CompetitionCollectionConfigUpdateRequest {

    @NotNull(message = "启用的收款方式不能为空")
    private List<String> enabledMethods;

    private Long wechatQrAssetId;
    private String bankAccountName;
    private String bankAccountNo;
    private String bankName;
    private String collectionNote;
}
