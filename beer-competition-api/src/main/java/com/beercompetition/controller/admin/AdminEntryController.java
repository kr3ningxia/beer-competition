package com.beercompetition.controller.admin;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.AdminEntryUpdateRequest;
import com.beercompetition.pojo.vo.AdminEntryDetailVO;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.beercompetition.service.EntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台报名管理接口，负责报名列表、详情、支付收样和退款处理入口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminEntryController {

    private final EntryService entryService;

    /**
     * 按复合条件分页查询后台报名列表。
     */
    @GetMapping("/entries")
    public Result<PageResult<AdminEntryVO>> entries(@RequestParam(required = false) Long competitionId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String paymentStatus,
                                                    @RequestParam(required = false) String deliveryStatus,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) Boolean assigned,
                                                    @RequestParam(required = false) String refundStatus,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer pageSize) {
        return Result.success(entryService.listAdminEntries(competitionId, status, paymentStatus, deliveryStatus,
                categoryId, assigned, refundStatus, keyword, page, pageSize));
    }

    /**
     * 查询后台报名详情。
     */
    @GetMapping("/entries/{id}")
    public Result<AdminEntryDetailVO> entryDetail(@PathVariable Long id) {
        return Result.success(entryService.getAdminEntry(id));
    }

    /**
     * 更新后台可维护的报名资料。
     */
    @PutMapping("/entries/{id}")
    public Result<AdminEntryDetailVO> updateEntry(@PathVariable Long id,
                                                  @RequestBody @Valid AdminEntryUpdateRequest request) {
        return Result.success(entryService.updateAdminEntry(id, request));
    }

    /**
     * 后台确认报名支付到账。
     */
    @PostMapping("/entries/{id}/confirm-payment")
    public Result<String> confirmEntryPayment(@PathVariable Long id,
                                              @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.confirmPayment(id, request);
        return Result.success("确认成功");
    }

    /**
     * 标记报名样品已入库。
     */
    @PostMapping("/entries/{id}/mark-stored")
    public Result<String> markEntryStored(@PathVariable Long id,
                                          @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.markStored(id, request);
        return Result.success("入库成功");
    }

    /**
     * 撤销报名样品入库状态。
     */
    @PostMapping("/entries/{id}/unmark-stored")
    public Result<String> unmarkEntryStored(@PathVariable Long id,
                                            @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.unmarkStored(id, request);
        return Result.success("撤销成功");
    }

    /**
     * 后台取消指定报名。
     */
    @PostMapping("/entries/{id}/cancel")
    public Result<String> cancelEntry(@PathVariable Long id,
                                      @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.cancelEntry(id, request);
        return Result.success("取消成功");
    }

    /**
     * 分页查询退款申请列表。
     */
    @GetMapping("/refunds")
    public Result<PageResult<AdminEntryVO>> refunds(@RequestParam(required = false) String status,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer pageSize) {
        return Result.success(entryService.listAdminRefunds(status, page, pageSize));
    }

    /**
     * 审核通过退款申请并推进退款。
     */
    @PostMapping("/refunds/{id}/approve")
    public Result<String> approveRefund(@PathVariable Long id,
                                        @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.approveRefund(id, request);
        return Result.success("退款成功");
    }

    /**
     * 驳回退款申请并保留报名。
     */
    @PostMapping("/refunds/{id}/reject")
    public Result<String> rejectRefund(@PathVariable Long id,
                                       @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.rejectRefund(id, request);
        return Result.success("已驳回退款");
    }

    /**
     * 对失败的退款申请发起重试。
     */
    @PostMapping("/refunds/{id}/retry")
    public Result<String> retryRefund(@PathVariable Long id,
                                      @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.retryRefund(id, request);
        return Result.success("退款重试成功");
    }
}
