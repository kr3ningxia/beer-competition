package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BeerCoinOverviewVO {

    private BeerCoinPricingVO activePricing;
    private BeerCoinWalletVO wallet;
    private List<BeerCoinPurchaseOrderVO> purchaseOrders;
    private List<BeerCoinLedgerVO> ledger;
    private List<BeerCoinLotVO> lots;
    private List<BeerCoinAccountOptionVO> accountOptions;
    private Boolean canConfigurePricing;
    private Boolean canPurchase;
}
