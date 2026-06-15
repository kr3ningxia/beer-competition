package com.beercompetition.controller.portal;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.PortalBankTransferSubmitRequest;
import com.beercompetition.pojo.vo.BankTransferAccountVO;
import com.beercompetition.pojo.vo.BankTransferVO;
import com.beercompetition.pojo.vo.BankTransferVoucherVO;
import com.beercompetition.pojo.vo.EntryPaymentStatusVO;
import com.beercompetition.pojo.vo.WechatNativePayVO;
import com.beercompetition.service.BankTransferPaymentService;
import com.beercompetition.service.WechatPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final WechatPaymentService wechatPaymentService;

    /**
     * 为指定报名酒款创建微信 Native 支付二维码。
     */
    @PostMapping("/entries/{id}/payment/wechat/native")
    public Result<WechatNativePayVO> createWechatNativePayment(@PathVariable Long id) {
        return Result.success(wechatPaymentService.createNativePayment(id));
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
     * 提交银行转账付款批次。
     */
    @PostMapping("/payment/bank-transfer")
    public Result<BankTransferVO> submitBankTransfer(@RequestBody @Valid PortalBankTransferSubmitRequest request) {
        return Result.success(bankTransferPaymentService.submitPortalTransfer(request));
    }

    /**
     * 查询当前厂商的银行转账批次详情。
     */
    @GetMapping("/payment/bank-transfer/{id}")
    public Result<BankTransferVO> bankTransferDetail(@PathVariable Long id) {
        return Result.success(bankTransferPaymentService.getPortalTransfer(id));
    }

    /**
     * 当前厂商取消未审核的银行转账批次。
     */
    @PostMapping("/payment/bank-transfer/{id}/cancel")
    public Result<BankTransferVO> cancelBankTransfer(@PathVariable Long id) {
        return Result.success(bankTransferPaymentService.cancelPortalTransfer(id));
    }
}
