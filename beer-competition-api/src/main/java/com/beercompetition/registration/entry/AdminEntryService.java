package com.beercompetition.registration.entry;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.pojo.dto.AdminEntryDeleteRequest;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.AdminEntryUpdateRequest;
import com.beercompetition.pojo.vo.AdminEntryDeleteImpactVO;
import com.beercompetition.pojo.vo.AdminEntryDetailVO;
import com.beercompetition.pojo.vo.AdminEntryVO;

/**
 * 提供后台报名查询、资料维护和报名状态管理。
 */
public interface AdminEntryService {

    PageResult<AdminEntryVO> listAdminEntries(Long competitionId, String status, String paymentStatus,
                                              String deliveryStatus, Long categoryId, Boolean assigned,
                                              String refundStatus, String keyword, Integer page, Integer pageSize);

    AdminEntryDetailVO getAdminEntry(Long entryId);

    AdminEntryDetailVO updateAdminEntry(Long entryId, AdminEntryUpdateRequest request);

    AdminEntryDeleteImpactVO getAdminEntryDeleteImpact(Long entryId);

    void administrativelyDeleteEntry(Long entryId, AdminEntryDeleteRequest request);

    void markStored(Long entryId, AdminEntryStatusRequest request);

    void markStored(Long entryId);

    void unmarkStored(Long entryId, AdminEntryStatusRequest request);

    void cancelEntry(Long entryId, AdminEntryStatusRequest request);

    void cancelEntry(Long entryId);
}
