package com.beercompetition.scheduler;

import com.beercompetition.service.WechatPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatRefundReconciliationScheduler {

    private final WechatPaymentService wechatPaymentService;

    @Scheduled(cron = "30 * * * * *")
    public void reconcileProcessingRefunds() {
        int reconciledCount = wechatPaymentService.reconcileProcessingRefunds();
        if (reconciledCount > 0) {
            log.info("Reconciled processing WeChat refunds, count={}", reconciledCount);
        }
    }
}
