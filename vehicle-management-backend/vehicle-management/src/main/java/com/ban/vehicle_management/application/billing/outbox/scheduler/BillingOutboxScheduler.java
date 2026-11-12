package com.ban.vehicle_management.application.billing.outbox.scheduler;

import com.ban.vehicle_management.application.billing.outbox.port.out.BillingOutboxPortOut;
import com.ban.vehicle_management.application.billing.outbox.usecase.BillingOutboxProcessor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BillingOutboxScheduler {

    private final BillingOutboxPortOut billingOutboxPortOut;
    private final BillingOutboxProcessor processor;
    private final int batchSize;
    private final int leaseSeconds;

    public BillingOutboxScheduler(
            BillingOutboxPortOut billingOutboxPortOut,
            BillingOutboxProcessor processor,
            @Value("${app.billing.outbox.batch-size:100}") int batchSize,
            @Value("${app.billing.outbox.lease-seconds:300}") int leaseSeconds) {
        this.billingOutboxPortOut = billingOutboxPortOut;
        this.processor = processor;
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
        this.leaseSeconds = Math.max(30, leaseSeconds);
    }

    @Scheduled(fixedDelayString = "${app.billing.outbox.fixed-delay-ms:5000}")
    public void processDue() {
        Instant now = Instant.now();
        for (UUID eventId : billingOutboxPortOut.claimDue(
                batchSize, now, now.minus(leaseSeconds, ChronoUnit.SECONDS))) {
            try {
                processor.process(eventId);
            } catch (Exception exception) {
                processor.fail(eventId, exception);
            }
        }
    }
}
