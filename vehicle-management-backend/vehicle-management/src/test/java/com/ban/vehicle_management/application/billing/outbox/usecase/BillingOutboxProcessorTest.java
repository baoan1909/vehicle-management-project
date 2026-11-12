package com.ban.vehicle_management.application.billing.outbox.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.outbox.port.out.BillingOutboxPortOut;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeAssessmentPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.in.SettlementPortIn;
import com.ban.vehicle_management.domain.billing.outbox.model.BillingOutboxEvent;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeAssessment;
import com.ban.vehicle_management.shared.enumeration.billing.BillingOutboxStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeAssessmentStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingOutboxProcessorTest {

    @Mock
    private BillingOutboxPortOut billingOutboxPortOut;
    @Mock
    private SettlementPortIn settlementPortIn;
    @Mock
    private PlatformFeeAssessmentPortOut assessmentPortOut;

    private BillingOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new BillingOutboxProcessor(
                billingOutboxPortOut, settlementPortIn, assessmentPortOut, 1);
    }

    @Test
    void processesPaymentEventIdempotentlyThroughSettlement() {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        when(billingOutboxPortOut.findById(eventId))
                .thenReturn(Optional.of(event(eventId, paymentId, BillingOutboxStatus.PROCESSING)));

        processor.process(eventId);

        verify(settlementPortIn).allocateForCompletedPayment(paymentId);
        verify(billingOutboxPortOut).markProcessed(any(), any(Instant.class));
    }

    @Test
    void skipsAlreadyProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        when(billingOutboxPortOut.findById(eventId)).thenReturn(Optional.of(
                event(eventId, UUID.randomUUID(), BillingOutboxStatus.PROCESSED)));

        processor.process(eventId);

        verify(settlementPortIn, never()).allocateForCompletedPayment(any());
        verify(billingOutboxPortOut, never()).markProcessed(any(), any());
    }

    @Test
    void deadLetterMarksAssessmentForReconciliation() {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PlatformFeeAssessment assessment = new PlatformFeeAssessment();
        when(billingOutboxPortOut.findById(eventId))
                .thenReturn(Optional.of(event(eventId, paymentId, BillingOutboxStatus.PROCESSING)));
        when(billingOutboxPortOut.markFailed(any(), any(), anyString(), anyInt())).thenReturn(true);
        when(assessmentPortOut.findByPaymentId(paymentId)).thenReturn(Optional.of(assessment));
        when(assessmentPortOut.save(assessment)).thenReturn(assessment);

        processor.fail(eventId, new IllegalStateException("sensitive gateway detail"));

        assertEquals(PlatformFeeAssessmentStatus.RECONCILIATION_REQUIRED, assessment.getStatus());
        verify(assessmentPortOut).save(assessment);
    }

    private BillingOutboxEvent event(UUID eventId, UUID paymentId, BillingOutboxStatus status) {
        BillingOutboxEvent event = new BillingOutboxEvent();
        event.setEventId(eventId);
        event.setAggregateType("PAYMENT");
        event.setAggregateId(paymentId);
        event.setEventType("PAYMENT_COMPLETED");
        event.setStatus(status);
        event.setRetryCount(0);
        return event;
    }
}
