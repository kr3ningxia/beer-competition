package com.beercompetition.controller.portal;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.PortalBankTransferSubmitRequest;
import com.beercompetition.pojo.dto.PortalPaymentOrderBankTransferRequest;
import com.beercompetition.pojo.dto.WechatJsapiPayRequest;
import com.beercompetition.pojo.vo.BankTransferAccountVO;
import com.beercompetition.pojo.vo.BankTransferVO;
import com.beercompetition.pojo.vo.BankTransferVoucherVO;
import com.beercompetition.pojo.vo.EntryPaymentStatusVO;
import com.beercompetition.pojo.vo.PaymentOrderStatusVO;
import com.beercompetition.pojo.vo.WechatJsapiPayVO;
import com.beercompetition.pojo.vo.WechatNativePayVO;
import com.beercompetition.pojo.vo.WechatPayClientConfigVO;
import com.beercompetition.service.BankTransferPaymentService;
import com.beercompetition.service.BatchPaymentService;
import com.beercompetition.service.WechatPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 厂商支付接口，处理微信 Native 支付和银行转账付款流程。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal")
public class PortalPaymentController {

    private final BankTransferPaymentService bankTransferPaymentService;
    private final BatchPaymentService batchPaymentService;
    private final WechatPaymentService wechatPaymentService;

    /**
     * 为多酒款报名订单创建一个微信 Native 支付二维码。
     */
    @PostMapping("/payment-orders/{id}/wechat/native")
    public Result<WechatNativePayVO> createBatchWechatNativePayment(@PathVariable Long id) {
        return Result.success(batchPaymentService.createNativePayment(id));
    }

    /**
     * 为多酒款报名订单创建微信 JSAPI 支付参数。
     */
    @PostMapping("/payment-orders/{id}/wechat/jsapi")
    public Result<WechatJsapiPayVO> createBatchWechatJsapiPayment(
            @PathVariable Long id,
            @RequestBody @Valid WechatJsapiPayRequest request) {
        return Result.success(batchPaymentService.createJsapiPayment(id, request.getCode()));
    }

    /**
     * 查询多酒款报名订单支付状态。
     */
    @GetMapping("/payment-orders/{id}/status")
    public Result<PaymentOrderStatusVO> batchPaymentStatus(@PathVariable Long id) {
        return Result.success(batchPaymentService.getPortalPaymentStatus(id));
    }

    /**
     * 测试环境模拟多酒款订单到账。
     */
    @PostMapping("/payment-orders/{id}/simulate")
    public Result<PaymentOrderStatusVO> simulateBatchPayment(@PathVariable Long id) {
        return Result.success(batchPaymentService.simulatePayment(id));
    }

    /**
     * 为指定报名酒款创建微信 Native 支付二维码。
     */
    @PostMapping("/entries/{id}/payment/wechat/native")
    public Result<WechatNativePayVO> createWechatNativePayment(@PathVariable Long id) {
        return Result.success(wechatPaymentService.createNativePayment(id));
    }

    /**
     * 为微信内置浏览器创建微信 JSAPI 支付订单。
     */
    @PostMapping("/entries/{id}/payment/wechat/jsapi")
    public Result<WechatJsapiPayVO> createWechatJsapiPayment(@PathVariable Long id,
                                                             @RequestBody @Valid WechatJsapiPayRequest request) {
        return Result.success(wechatPaymentService.createJsapiPayment(id, request.getCode()));
    }

    @GetMapping("/payment/wechat/client-config")
    public Result<WechatPayClientConfigVO> wechatPaymentClientConfig() {
        return Result.success(wechatPaymentService.getClientConfig());
    }

    /**
     * 查询指定报名酒款的支付状态。
     */
    @GetMapping("/entries/{id}/payment/status")
    public Result<EntryPaymentStatusVO> entryPaymentStatus(@PathVariable Long id) {
        return Result.success(wechatPaymentService.getPortalPaymentStatus(id));
    }

    /**
     * 查询银行转账收款账户信息。
     */
    @GetMapping("/payment/bank-transfer/account")
    public Result<BankTransferAccountVO> bankTransferAccount() {
        return Result.success(bankTransferPaymentService.getAccount());
    }

    /**
     * 上传银行转账付款凭证。
     */
    @PostMapping(value = "/payment/bank-transfer/voucher", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<BankTransferVoucherVO> uploadBankTransferVoucher(@RequestParam("file") MultipartFile file) {
        return Result.success(bankTransferPaymentService.uploadVoucher(file));
    }

    /**
     * 提交单款酒的银行转账付款信息。
     */
    @PostMapping("/payment/bank-transfer")
    public Result<BankTransferVO> submitBankTransfer(@RequestBody @Valid PortalBankTransferSubmitRequest request) {
        return Result.success(bankTransferPaymentService.submitPortalTransfer(request));
    }

    /**
     * 提交多酒款聚合订单的银行转账付款信息。
     */
    @PostMapping("/payment-orders/{id}/bank-transfer")
    public Result<BankTransferVO> submitBatchBankTransfer(
            @PathVariable Long id,
            @RequestBody @Valid PortalPaymentOrderBankTransferRequest request) {
        return Result.success(bankTransferPaymentService.submitPortalOrderTransfer(id, request));
    }

    /**
     * 修改多酒款聚合订单待确认的银行转账信息。
     */
    @PutMapping("/payment-orders/{id}/bank-transfer")
    public Result<BankTransferVO> updateBatchBankTransfer(
            @PathVariable Long id,
            @RequestBody @Valid PortalPaymentOrderBankTransferRequest request) {
        return Result.success(bankTransferPaymentService.updatePortalOrderTransfer(id, request));
    }

    /**
     * 查询当前厂商的银行转账记录详情。
     */
    @GetMapping("/payment/bank-transfer/{id}")
    public Result<BankTransferVO> bankTransferDetail(@PathVariable Long id) {
        return Result.success(bankTransferPaymentService.getPortalTransfer(id));
    }

    /**
     * 修改当前厂商待确认的银行转账付款信息。
     */
    @PutMapping("/payment/bank-transfer/{id}")
    public Result<BankTransferVO> updateBankTransfer(@PathVariable Long id,
                                                     @RequestBody @Valid PortalBankTransferSubmitRequest request) {
        return Result.success(bankTransferPaymentService.updatePortalTransfer(id, request));
    }

    /**
     * 当前厂商取消未审核的银行转账记录。
     */
    @PostMapping("/payment/bank-transfer/{id}/cancel")
    public Result<BankTransferVO> cancelBankTransfer(@PathVariable Long id) {
        return Result.success(bankTransferPaymentService.cancelPortalTransfer(id));
    }
}
