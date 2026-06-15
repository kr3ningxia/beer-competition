package com.beercompetition.controller.judge;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.vo.JudgeEntryVO;
import com.beercompetition.service.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评委酒款接口，提供匿名酒款查看和现场扫码解析。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/judge")
public class JudgeEntryController {

    private final EntryService entryService;

    /**
     * 根据匿名酒款标识查询评委可见酒款信息。
     */
    @GetMapping("/entries/{uuid}")
    public Result<JudgeEntryVO> entry(@PathVariable String uuid) {
        return Result.success(entryService.getJudgeEntry(uuid));
    }

    /**
     * 解析二维码、短编号或匿名标签编码对应的酒款。
     */
    @GetMapping("/scan/resolve")
    public Result<JudgeEntryVO> resolveScan(@RequestParam String code) {
        return Result.success(entryService.resolveJudgeScan(code));
    }
}
