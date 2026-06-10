package com.beercompetition.scheduler;

import com.beercompetition.service.CompetitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionRegistrationScheduler {

    private final CompetitionService competitionService;

    @Scheduled(cron = "0 * * * * *")
    public void closeExpiredRegistrations() {
        int closedCount = competitionService.closeExpiredRegistrations();
        if (closedCount > 0) {
            log.info("Auto closed expired competition registrations, count={}", closedCount);
        }
    }
}
