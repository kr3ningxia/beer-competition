package com.beercompetition.service;

import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.pojo.vo.EntryPaymentStatusVO;
import com.beercompetition.pojo.vo.WechatJsapiPayVO;
import com.beercompetition.pojo.vo.WechatNativePayVO;
import com.beercompetition.pojo.vo.WechatPayClientConfigVO;

public interface WechatPaymentService {

    WechatNativePayVO createNativePayment(Long entryId);

    WechatJsapiPayVO createJsapiPayment(Long entryId, String code);

    WechatPayClientConfigVO getClientConfig();

    EntryPaymentStatusVO getPortalPaymentStatus(Long entryId);

    void handlePaymentNotify(WechatPayClient.WechatNotifyRequest request);

    void handleRefundNotify(WechatPayClient.WechatNotifyRequest request);

    void approveRefund(Long refundId, String reason, Long adminId);

    void autoApproveRefund(Long refundId, String reason);

    void retryRefund(Long refundId, String reason, Long adminId);
}
