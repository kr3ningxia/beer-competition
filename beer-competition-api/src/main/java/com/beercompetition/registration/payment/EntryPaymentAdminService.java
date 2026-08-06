package com.beercompetition.registration.payment;

import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.vo.EntryDetailVO;

/**
 * 提供一期单酒款支付模拟和后台到账确认入口。
 */
public interface EntryPaymentAdminService {

    EntryDetailVO simulatePayment(Long entryId);

    void confirmPayment(Long entryId, AdminEntryStatusRequest request);

    void confirmPayment(Long entryId);
}
