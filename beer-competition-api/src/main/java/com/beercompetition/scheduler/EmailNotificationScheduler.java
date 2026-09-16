package com.beercompetition.scheduler;

import com.beercompetition.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationScheduler {

    private final EmailNotificationService emailNotificationService;

    @Scheduled(cron = "15 * * * * *")
    public void generateNotifications() {
        int count = emailNotificationService.generateScheduledNotifications(LocalDateTime.now());
        if (count > 0) {
            log.info("Generated competition email notifications, count={}", count);
        }
    }

    @Scheduled(cron = "30 * * * * *")
    public void sendNotifications() {
        int count = emailNotificationService.sendPendingEmails(LocalDateTime.now());
        if (count > 0) {
            log.info("Sent competition email notifications, count={}", count);
        }
    }
}
