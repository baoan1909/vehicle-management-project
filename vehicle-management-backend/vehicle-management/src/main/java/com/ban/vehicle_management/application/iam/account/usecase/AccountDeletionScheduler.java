package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountDeletionScheduler {

    private final AccountDeletionPortOut persistence;
    private final AccountDeletionProcessor processor;
    private final AccountDeletionTimeProperties properties;
    private final Clock clock;

    public AccountDeletionScheduler(AccountDeletionPortOut persistence, AccountDeletionProcessor processor,
            AccountDeletionTimeProperties properties, Clock clock) {
        this.persistence = persistence;
        this.processor = processor;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.iam.account-deletion.scheduler-delay:30s}",
            initialDelayString = "${app.iam.account-deletion.scheduler-initial-delay:30s}")
    public void processDueRequests() {
        Instant now = Instant.now(clock);
        String claimToken = UUID.randomUUID().toString();
        List<UUID> claimed = persistence.claimDue(now, now.plus(properties.getProcessingLease()),
                claimToken, properties.getBatchSize());
        claimed.forEach(requestId -> processor.process(requestId, claimToken));
    }
}
