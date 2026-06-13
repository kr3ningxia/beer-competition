package com.beercompetition.service;

import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.pojo.vo.EntryPaymentStatusVO;
import com.beercompetition.pojo.vo.WechatNativePayVO;

public interface WechatPaymentService {

    WechatNativePayVO createNativePayment(Long entryId);

    EntryPaymentStatusVO getPortalPaymentStatus(Long entryId);

    void handlePaymentNotify(WechatPayClient.WechatNotifyRequest request);

    void handleRefundNotify(WechatPayClient.WechatNotifyRequest request);

    void approveRefund(Long refundId, String reason, Long adminId);

    void retryRefund(Long refundId, String reason, Long adminId);
}
