package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BeerCoinWalletVO {

    private Long enterpriseAccountId;
    private String enterpriseAccountName;
    private Long availableQuantity;
    private Long expiringQuantity;
    private LocalDateTime nextExpiryTime;
}
