package com.beercompetition.controller.admin;

import com.beercompetition.billing.beercoin.BeerCoinService;
import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.BeerCoinAdjustmentRequest;
import com.beercompetition.pojo.dto.BeerCoinPricingRequest;
import com.beercompetition.pojo.dto.BeerCoinPurchaseRequest;
import com.beercompetition.pojo.dto.WechatJsapiPayRequest;
import com.beercompetition.pojo.vo.BeerCoinLedgerVO;
import com.beercompetition.pojo.vo.BeerCoinOverviewVO;
import com.beercompetition.pojo.vo.BeerCoinPricingVO;
import com.beercompetition.pojo.vo.BeerCoinPurchaseOrderVO;
import com.beercompetition.pojo.vo.BeerCoinPurchasePaymentVO;
import com.beercompetition.pojo.vo.BeerCoinSettlementVO;
import com.beercompetition.pojo.vo.WechatPayClientConfigVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台啤酒币定价、购买和账务查询接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/beer-coins")
public class AdminBeerCoinController {

    private final BeerCoinService beerCoinService;

    @GetMapping
    public Result<BeerCoinOverviewVO> overview() {
        return Result.success(beerCoinService.getOverview());
    }

    @GetMapping("/pricing")
    public Result<BeerCoinPricingVO> activePricing() {
        return Result.success(beerCoinService.getActivePricing());
    }

    @PutMapping("/pricing")
    public Result<BeerCoinPricingVO> savePricing(@RequestBody @Valid BeerCoinPricingRequest request) {
        return Result.success(beerCoinService.savePricing(request));
    }

    /**
     * 兼容旧版控制台及未重启的后端实例，价格设置仍统一走同一保存服务。
     */
    @PostMapping("/pricing")
    public Result<BeerCoinPricingVO> savePricingLegacy(@RequestBody @Valid BeerCoinPricingRequest request) {
        return Result.success(beerCoinService.savePricing(request));
    }

    @PostMapping("/purchase-orders")
    public Result<BeerCoinPurchaseOrderVO> createPurchaseOrder(@RequestBody @Valid BeerCoinPurchaseRequest request) {
        return Result.success(beerCoinService.createPurchaseOrder(request));
    }

    @GetMapping("/purchase-orders")
    public Result<List<BeerCoinPurchaseOrderVO>> purchaseOrders() {
        return Result.success(beerCoinService.listPurchaseOrders());
    }

    @GetMapping("/purchase-orders/{id}")
    public Result<BeerCoinPurchaseOrderVO> purchaseOrder(@PathVariable Long id) {
        return Result.success(beerCoinService.getPurchaseOrder(id));
    }

    @PostMapping("/purchase-orders/{id}/wechat/native")
    public Result<BeerCoinPurchasePaymentVO> createNativePayment(@PathVariable Long id) {
        return Result.success(beerCoinService.createNativePayment(id));
    }

    @PostMapping("/purchase-orders/{id}/wechat/jsapi")
    public Result<BeerCoinPurchasePaymentVO> createJsapiPayment(@PathVariable Long id,
                                                                  @RequestBody @Valid WechatJsapiPayRequest request) {
        return Result.success(beerCoinService.createJsapiPayment(id, request.getCode()));
    }

    @GetMapping("/wechat/client-config")
    public Result<WechatPayClientConfigVO> wechatClientConfig() {
        return Result.success(beerCoinService.getWechatPayClientConfig());
    }

    /**
     * 测试环境模拟啤酒币购买到账，真实微信模式下由后端拒绝。
     */
    @PostMapping("/purchase-orders/{id}/simulate")
    public Result<BeerCoinPurchaseOrderVO> simulatePayment(@PathVariable Long id) {
        return Result.success(beerCoinService.simulatePayment(id));
    }

    @GetMapping("/purchase-orders/{id}/status")
    public Result<BeerCoinPurchaseOrderVO> purchaseOrderStatus(@PathVariable Long id) {
        return Result.success(beerCoinService.getPurchaseOrderStatus(id));
    }

    @GetMapping("/ledger")
    public Result<List<BeerCoinLedgerVO>> ledger() {
        return Result.success(beerCoinService.listLedger());
    }

    @GetMapping("/lots")
    public Result<?> lots() {
        return Result.success(beerCoinService.listLots());
    }

    @PostMapping("/adjustments")
    public Result<BeerCoinLedgerVO> adjust(@RequestBody @Valid BeerCoinAdjustmentRequest request) {
        return Result.success(beerCoinService.adjust(request));
    }

    @GetMapping("/settlements/{competitionId}")
    public Result<BeerCoinSettlementVO> settlement(@PathVariable Long competitionId) {
        return Result.success(beerCoinService.getSettlement(competitionId));
    }
}
