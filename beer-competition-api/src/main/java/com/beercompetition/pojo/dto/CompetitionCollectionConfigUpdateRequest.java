package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 128, message = "付款咨询联系最多128个字")
    private String paymentContact;
}
