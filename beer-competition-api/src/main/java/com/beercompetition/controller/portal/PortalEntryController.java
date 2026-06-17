package com.beercompetition.controller.portal;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.PortalEntryDeliverySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.PortalEntryLabelVO;
import com.beercompetition.pojo.vo.PortalMyParticipationVO;
import com.beercompetition.service.EntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 厂商报名接口，承接报名提交、详情查看、寄样和退款申请。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal")
public class PortalEntryController {

    private final EntryService entryService;

    /**
     * 查询当前厂商的报名酒款列表。
     */
    @GetMapping("/entries")
    public Result<List<EntrySummaryVO>> entries() {
        return Result.success(entryService.listPortalEntries());
    }

    /**
     * 查询当前厂商的报名酒款详情。
     */
    @GetMapping("/entries/{id}")
    public Result<EntryDetailVO> entryDetail(@PathVariable Long id) {
        return Result.success(entryService.getPortalEntry(id));
    }

    /**
     * 提交报名酒款寄样信息。
     */
    @PostMapping("/entries/{id}/delivery")
    public Result<EntryDetailVO> submitEntryDelivery(@PathVariable Long id,
                                                     @RequestBody @Valid PortalEntryDeliverySubmitRequest request) {
        return Result.success(entryService.submitPortalEntryDelivery(id, request));
    }

    /**
     * 当前厂商取消待支付的报名酒款。
     */
    @PostMapping("/entries/{id}/cancel")
    public Result<EntryDetailVO> cancelEntry(@PathVariable Long id) {
        return Result.success(entryService.cancelPortalEntry(id));
    }

    /**
     * 测试环境模拟报名支付到账。
     */
    @PostMapping("/entries/{id}/payment/simulate")
    public Result<EntryDetailVO> simulatePayment(@PathVariable Long id) {
        return Result.success(entryService.simulatePayment(id));
    }

    /**
     * 当前厂商对报名酒款发起退款申请。
     */
    @PostMapping("/entries/{id}/refund")
    public Result<EntryDetailVO> requestRefund(@PathVariable Long id,
                                               @RequestBody @Valid PortalEntryRefundRequest request) {
        return Result.success(entryService.requestPortalEntryRefund(id, request));
    }

    /**
     * 向指定比赛提交新的报名酒款。
     */
    @PostMapping("/competitions/{competitionId}/entries")
    public Result<EntryDetailVO> submitEntry(@PathVariable Long competitionId,
                                             @RequestBody @Valid PortalEntrySubmitRequest request) {
        return Result.success(entryService.submitPortalEntry(competitionId, request));
    }

    /**
     * 获取报名酒款瓶贴信息。
     */
    @GetMapping("/entries/{id}/label")
    public Result<PortalEntryLabelVO> entryLabel(@PathVariable Long id) {
        return Result.success(entryService.getPortalEntryLabel(id));
    }

    /**
     * 查询当前厂商参赛首页聚合信息。
     */
    @GetMapping("/my")
    public Result<PortalMyParticipationVO> myParticipation() {
        return Result.success(entryService.getPortalMyParticipation());
    }
}
