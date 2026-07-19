package com.beercompetition.service;

import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.pojo.vo.PaymentOrderStatusVO;
import com.beercompetition.pojo.vo.WechatJsapiPayVO;
import com.beercompetition.pojo.vo.WechatNativePayVO;

public interface BatchPaymentService {

    WechatNativePayVO createNativePayment(Long orderId);

    WechatJsapiPayVO createJsapiPayment(Long orderId, String code);

    PaymentOrderStatusVO getPortalPaymentStatus(Long orderId);

    PaymentOrderStatusVO simulatePayment(Long orderId);

    boolean applyWechatPaymentSuccess(WechatPayClient.PaymentNotifyResult result);

    void markBankTransferPending(Long orderId, Long transferId);

    void confirmBankTransfer(Long orderId, Long transferId, Long adminId);

    void resetBankTransfer(Long orderId, Long transferId);
}
