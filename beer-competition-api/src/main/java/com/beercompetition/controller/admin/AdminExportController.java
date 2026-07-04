package com.beercompetition.controller.admin;

import com.beercompetition.controller.support.FileResponseHelper;
import com.beercompetition.service.AdminExportService;
import com.beercompetition.service.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台导出接口，集中处理评分、报名、收样和瓶贴文件下载。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/competitions/{id}/exports")
public class AdminExportController {

    private final AdminExportService adminExportService;
    private final CompetitionService competitionService;

    /**
     * 导出评审使用的比赛评分数据，并记录导出操作。
     */
    @GetMapping("/scoring")
    public ResponseEntity<byte[]> exportScoringData(@PathVariable Long id) {
        byte[] content = competitionService.exportScoringData(id);
        adminExportService.logScoringExport(id);
        return FileResponseHelper.xlsx("scoring-data.xlsx", content);
    }

    /**
     * 按筛选条件导出报名台账。
     */
    @GetMapping("/entries")
    public ResponseEntity<byte[]> exportEntries(@PathVariable Long id,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) String entryStatus,
                                                @RequestParam(required = false) String paymentStatus,
                                                @RequestParam(required = false) String deliveryStatus,
                                                @RequestParam(required = false) String keyword) {
        return FileResponseHelper.attachment(adminExportService.exportEntries(
                id, categoryId, entryStatus, paymentStatus, deliveryStatus, keyword));
    }

    /**
     * 按筛选条件导出收样入库清单。
     */
    @GetMapping("/delivery")
    public ResponseEntity<byte[]> exportDelivery(@PathVariable Long id,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) String entryStatus,
                                                 @RequestParam(required = false) String paymentStatus,
                                                 @RequestParam(required = false) String deliveryStatus,
                                                 @RequestParam(required = false) String keyword) {
        return FileResponseHelper.attachment(adminExportService.exportDelivery(
                id, categoryId, entryStatus, paymentStatus, deliveryStatus, keyword));
    }

    /**
     * 按筛选条件导出瓶贴文件包。
     */
    @GetMapping("/labels")
    public ResponseEntity<byte[]> exportLabels(@PathVariable Long id,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false) String entryStatus,
                                               @RequestParam(required = false) String paymentStatus,
                                               @RequestParam(required = false) String deliveryStatus,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer copies,
                                               @RequestParam(required = false) String format) {
        return FileResponseHelper.attachment(adminExportService.exportLabels(
                id, categoryId, entryStatus, paymentStatus, deliveryStatus, keyword, copies, format));
    }
}
