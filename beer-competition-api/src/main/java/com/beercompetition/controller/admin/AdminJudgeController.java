package com.beercompetition.controller.admin;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.AdminJudgePhoneUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeUpdateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentCreateRequest;
import com.beercompetition.pojo.vo.JudgeAccountVO;
import com.beercompetition.service.JudgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台评委管理接口，负责评委资料、审核状态和比赛分配入口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminJudgeController {

    private final JudgeService judgeService;

    /**
     * 查询评委列表，适用于下拉选择等轻量场景。
     */
    @GetMapping("/judges")
    public Result<List<JudgeAccountVO>> judges(@RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(judgeService.listJudges(status, keyword));
    }

    /**
     * 分页查询后台评委池。
     */
    @GetMapping("/judges/page")
    public Result<PageResult<JudgeAccountVO>> judgesPage(@RequestParam(required = false) Integer status,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer pageSize) {
        return Result.success(judgeService.pageJudges(status, keyword, page, pageSize));
    }

    /**
     * 查询评委完整资料详情。
     */
    @GetMapping("/judges/{publicId}")
    public Result<JudgeAccountVO> judgeDetail(@PathVariable String publicId) {
        return Result.success(judgeService.getJudgeDetail(publicId));
    }

    /**
     * 更新评委基础资料。
     */
    @PutMapping("/judges/{publicId}")
    public Result<JudgeAccountVO> updateJudge(@PathVariable String publicId,
                                              @RequestBody @Valid AdminJudgeUpdateRequest request) {
        return Result.success(judgeService.updateJudge(publicId, request));
    }

    /**
     * 后台修改评委手机号。
     */
    @PatchMapping("/judges/{publicId}/phone")
    public Result<JudgeAccountVO> updateJudgePhone(@PathVariable String publicId,
                                                   @RequestBody @Valid AdminJudgePhoneUpdateRequest request) {
        return Result.success(judgeService.updateJudgePhone(publicId, request));
    }

    /**
     * 更新评委审核或启用状态。
     */
    @PatchMapping("/judges/{publicId}/status")
    public Result<JudgeAccountVO> updateJudgeStatus(@PathVariable String publicId,
                                                    @RequestBody @Valid AdminJudgeStatusUpdateRequest request) {
        return Result.success(judgeService.updateJudgeStatus(publicId, request));
    }

    /**
     * 为单个评委创建或更新比赛桌次分配。
     */
    @PostMapping("/judge-assignments")
    public Result<String> createAssignment(@RequestBody @Valid JudgeAssignmentCreateRequest request) {
        judgeService.createAssignment(request);
        return Result.success("分配成功");
    }

    /**
     * 批量保存指定比赛的评委编排。
     */
    @PutMapping("/competitions/{id}/judge-assignments")
    public Result<String> updateCompetitionAssignments(@PathVariable Long id,
                                                       @RequestBody @Valid JudgeAssignmentBatchUpdateRequest request) {
        judgeService.updateCompetitionAssignments(id, request);
        return Result.success("保存成功");
    }
}
