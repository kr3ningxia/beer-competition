package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BeerCoinLedgerVO {

    private Long id;
    private String ledgerNo;
    private Long enterpriseAccountId;
    private String enterpriseAccountName;
    private String direction;
    private Long quantity;
    private String businessType;
    private String businessId;
    private String reason;
    private LocalDateTime createTime;
}
