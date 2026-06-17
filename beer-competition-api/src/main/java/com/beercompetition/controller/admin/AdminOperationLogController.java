package com.beercompetition.controller.admin;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.vo.AdminOperationLogVO;
import com.beercompetition.service.AdminOperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台操作日志接口，提供关键业务操作的只读审计查询。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminOperationLogController {

    private final AdminOperationLogService adminOperationLogService;

    /**
     * 分页查询后台操作日志。
     */
    @GetMapping("/operation-logs")
    public Result<PageResult<AdminOperationLogVO>> operationLogs(@RequestParam(required = false) String startTime,
                                                                @RequestParam(required = false) String endTime,
                                                                @RequestParam(required = false) Long adminUserId,
                                                                @RequestParam(required = false) String targetType,
                                                                @RequestParam(required = false) String actionGroup,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) Integer page,
                                                                @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminOperationLogService.listAdminOperationLogs(startTime, endTime, adminUserId,
                targetType, actionGroup, keyword, page, pageSize));
    }
}
