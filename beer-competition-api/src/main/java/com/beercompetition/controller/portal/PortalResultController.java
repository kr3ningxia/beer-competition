package com.beercompetition.controller.portal;

import com.beercompetition.common.result.Result;
import com.beercompetition.controller.support.FileResponseHelper;
import com.beercompetition.pojo.vo.PortalResultDetailVO;
import com.beercompetition.pojo.vo.PortalResultSummaryVO;
import com.beercompetition.service.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 厂商成绩接口，提供当前厂商报名酒款的成绩和证书下载。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/results")
public class PortalResultController {

    private final EntryService entryService;

    /**
     * 查询当前厂商已可查看的成绩列表。
     */
    @GetMapping
    public Result<List<PortalResultSummaryVO>> results() {
        return Result.success(entryService.listPortalResults());
    }

    /**
     * 查询当前厂商指定酒款的成绩详情。
     */
    @GetMapping("/{entryId}")
    public Result<PortalResultDetailVO> resultDetail(@PathVariable Long entryId) {
        return Result.success(entryService.getPortalResultDetail(entryId));
    }

    /**
     * 下载当前厂商指定酒款的获奖证书。
     */
    @GetMapping("/{entryId}/certificate")
    public ResponseEntity<byte[]> resultCertificate(@PathVariable Long entryId) {
        return FileResponseHelper.attachment(entryService.downloadPortalResultCertificate(entryId));
    }
}
