package com.beercompetition.controller.admin;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.common.result.Result;
import com.beercompetition.controller.support.FileResponseHelper;
import com.beercompetition.pojo.dto.AdminBankTransferProcessRequest;
import com.beercompetition.pojo.vo.BankTransferVO;
import com.beercompetition.service.BankTransferPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台银行转账审核接口，处理厂商线下转账批次。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/bank-transfers")
public class AdminPaymentController {

    private final BankTransferPaymentService bankTransferPaymentService;

    /**
     * 分页查询银行转账批次列表。
     */
    @GetMapping
    public Result<PageResult<BankTransferVO>> bankTransfers(@RequestParam(required = false) String status,
                                                            @RequestParam(required = false) Long competitionId,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Integer page,
                                                            @RequestParam(required = false) Integer pageSize) {
        return Result.success(bankTransferPaymentService.listAdminTransfers(status, competitionId, keyword, page, pageSize));
    }

    /**
     * 查询银行转账批次详情。
     */
    @GetMapping("/{id}")
    public Result<BankTransferVO> bankTransferDetail(@PathVariable Long id) {
        return Result.success(bankTransferPaymentService.getAdminTransfer(id));
    }

    /**
     * 下载厂商上传的转账凭证。
     */
    @GetMapping("/{id}/voucher")
    public ResponseEntity<byte[]> downloadBankTransferVoucher(@PathVariable Long id) {
        return FileResponseHelper.attachment(bankTransferPaymentService.downloadVoucher(id));
    }

    /**
     * 审核通过银行转账批次。
     */
    @PostMapping("/{id}/confirm")
    public Result<BankTransferVO> confirmBankTransfer(@PathVariable Long id,
                                                      @RequestBody(required = false) @Valid AdminBankTransferProcessRequest request) {
        return Result.success(bankTransferPaymentService.confirmTransfer(id, request));
    }

    /**
     * 驳回银行转账批次。
     */
    @PostMapping("/{id}/reject")
    public Result<BankTransferVO> rejectBankTransfer(@PathVariable Long id,
                                                     @RequestBody(required = false) @Valid AdminBankTransferProcessRequest request) {
        return Result.success(bankTransferPaymentService.rejectTransfer(id, request));
    }
}
