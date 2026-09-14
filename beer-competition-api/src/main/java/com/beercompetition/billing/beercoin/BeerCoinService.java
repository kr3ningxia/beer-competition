package com.beercompetition.billing.beercoin;

import com.beercompetition.pojo.dto.BeerCoinAdjustmentRequest;
import com.beercompetition.pojo.dto.BeerCoinPricingRequest;
import com.beercompetition.pojo.dto.BeerCoinPurchaseRequest;
import com.beercompetition.pojo.vo.BeerCoinLedgerVO;
import com.beercompetition.pojo.vo.BeerCoinLotVO;
import com.beercompetition.pojo.vo.BeerCoinOverviewVO;
import com.beercompetition.pojo.vo.BeerCoinPricingVO;
import com.beercompetition.pojo.vo.BeerCoinPurchaseOrderVO;
import com.beercompetition.pojo.vo.BeerCoinPurchasePaymentVO;
import com.beercompetition.pojo.vo.BeerCoinSettlementVO;
import com.beercompetition.pojo.vo.BeerCoinWalletVO;
import com.beercompetition.pojo.vo.WechatPayClientConfigVO;
import com.beercompetition.pay.WechatPayClient;

import java.util.List;

public interface BeerCoinService {

    BeerCoinOverviewVO getOverview();

    BeerCoinWalletVO getWallet();

    BeerCoinPricingVO getActivePricing();

    BeerCoinPricingVO savePricing(BeerCoinPricingRequest request);

    BeerCoinPurchaseOrderVO createPurchaseOrder(BeerCoinPurchaseRequest request);

    List<BeerCoinPurchaseOrderVO> listPurchaseOrders();

    BeerCoinPurchaseOrderVO getPurchaseOrder(Long id);

    BeerCoinPurchasePaymentVO createNativePayment(Long id);

    BeerCoinPurchasePaymentVO createJsapiPayment(Long id, String code);

    WechatPayClientConfigVO getWechatPayClientConfig();

    BeerCoinPurchaseOrderVO simulatePayment(Long id);

    BeerCoinPurchaseOrderVO getPurchaseOrderStatus(Long id);

    List<BeerCoinLedgerVO> listLedger();

    List<BeerCoinLotVO> listLots();

    BeerCoinLedgerVO adjust(BeerCoinAdjustmentRequest request);

    BeerCoinSettlementVO getSettlement(Long competitionId);

    boolean isPurchaseOrder(String outTradeNo);

    boolean applyWechatPaymentSuccess(WechatPayClient.PaymentNotifyResult result);
}
