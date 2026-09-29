package com.beercompetition.scheduler;

import com.beercompetition.service.BatchPaymentService;
import com.beercompetition.service.WechatPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 兜底落过期状态：厂商离开付款页后不会再触发惰性过期，需要定时扫描。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOrderExpiryScheduler {

    private final BatchPaymentService batchPaymentService;

    private final WechatPaymentService wechatPaymentService;

    @Scheduled(cron = "0 * * * * *")
    public void expireOverduePayments() {
        int orders = batchPaymentService.expireOverdueOrders();
        int entries = wechatPaymentService.expireOverdueEntryPayments();
        if (orders > 0 || entries > 0) {
            log.info("Expired overdue payments, aggregateOrders={}, entryPayments={}", orders, entries);
        }
    }
}
