package com.beercompetition.service;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.pojo.vo.AdminOperationLogVO;

public interface AdminOperationLogService {

    PageResult<AdminOperationLogVO> listAdminOperationLogs(String startTime,
                                                           String endTime,
                                                           Long adminUserId,
                                                           String targetType,
                                                           String actionGroup,
                                                           String keyword,
                                                           Integer page,
                                                           Integer pageSize);
}
