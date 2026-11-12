package com.ban.vehicle_management.application.billing.outbox.usecase;

import com.ban.vehicle_management.application.billing.outbox.port.out.BillingOutboxPortOut;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeAssessmentPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.in.SettlementPortIn;
import com.ban.vehicle_management.domain.billing.outbox.model.BillingOutboxEvent;
import com.ban.vehicle_management.shared.enumeration.billing.BillingOutboxStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeAssessmentStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingOutboxProcessor {

    private final BillingOutboxPortOut billingOutboxPortOut;
    private final SettlementPortIn settlementPortIn;
    private final PlatformFeeAssessmentPortOut assessmentPortOut;
    private final int maxRetries;

    public BillingOutboxProcessor(
            BillingOutboxPortOut billingOutboxPortOut,
            SettlementPortIn settlementPortIn,
            PlatformFeeAssessmentPortOut assessmentPortOut,
            @Value("${app.billing.outbox.max-retries:8}") int maxRetries) {
        this.billingOutboxPortOut = billingOutboxPortOut;
        this.settlementPortIn = settlementPortIn;
        this.assessmentPortOut = assessmentPortOut;
        this.maxRetries = Math.max(1, maxRetries);
    }

    @Transactional
    public void process(UUID eventId) {
        BillingOutboxEvent event = requireEvent(eventId);
        if (BillingOutboxStatus.PROCESSED.equals(event.getStatus())) {
            return;
        }
        if (!"PAYMENT_COMPLETED".equals(event.getEventType())
                || !"PAYMENT".equals(event.getAggregateType())) {
            throw new IllegalStateException("Unsupported billing outbox event");
        }
        settlementPortIn.allocateForCompletedPayment(event.getAggregateId());
        billingOutboxPortOut.markProcessed(eventId, Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID eventId, Exception exception) {
        BillingOutboxEvent event = requireEvent(eventId);
        long delaySeconds = Math.min(3600L, 30L * (1L << Math.min(event.getRetryCount(), 6)));
        String error = exception.getClass().getSimpleName() + ": downstream billing processing failed";
        boolean deadLetter = billingOutboxPortOut.markFailed(
                eventId, Instant.now().plus(delaySeconds, ChronoUnit.SECONDS), error, maxRetries);
        if (deadLetter) {
            assessmentPortOut.findByPaymentId(event.getAggregateId()).ifPresent(assessment -> {
                assessment.setStatus(PlatformFeeAssessmentStatus.RECONCILIATION_REQUIRED);
                assessment.setUpdatedAt(Instant.now());
                assessmentPortOut.save(assessment);
            });
        }
    }

    private BillingOutboxEvent requireEvent(UUID eventId) {
        return billingOutboxPortOut.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Billing outbox event not found"));
    }
}
