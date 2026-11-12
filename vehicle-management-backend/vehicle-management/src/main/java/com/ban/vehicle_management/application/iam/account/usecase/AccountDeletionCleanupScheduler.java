package com.ban.vehicle_management.application.iam.account.usecase;

import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountDeletionCleanupScheduler {

    private final AccountDeletionExternalCleanupProcessor processor;
    private final Clock clock;

    public AccountDeletionCleanupScheduler(
            AccountDeletionExternalCleanupProcessor processor, Clock clock) {
        this.processor = processor;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.iam.account-deletion.scheduler-delay:30s}",
            initialDelayString = "${app.iam.account-deletion.scheduler-initial-delay:30s}")
    public void processDueCleanupItems() {
        processor.processCompletedRequests(Instant.now(clock));
    }
}
