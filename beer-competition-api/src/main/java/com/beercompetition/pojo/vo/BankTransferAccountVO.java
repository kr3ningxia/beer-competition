package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankTransferAccountVO {

    private String accountName;
    private String bankName;
    private String accountNo;
    private String remarkTip;
    private String invoiceTip;
    private String serviceWechat;
}
