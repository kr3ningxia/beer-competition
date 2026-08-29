package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BeerCoinLotVO {

    private Long id;
    private String lotNo;
    private Long enterpriseAccountId;
    private String enterpriseAccountName;
    private Long purchaseOrderId;
    private Long totalQuantity;
    private Long remainingQuantity;
    private LocalDateTime availableFrom;
    private LocalDateTime expiresAt;
    private String sourceType;
    private String sourceOrderId;
}
