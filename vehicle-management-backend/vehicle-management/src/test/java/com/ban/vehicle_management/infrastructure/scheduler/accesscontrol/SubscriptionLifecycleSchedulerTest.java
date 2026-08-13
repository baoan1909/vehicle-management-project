package com.ban.vehicle_management.infrastructure.scheduler.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionExpirationPortIn;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPaymentTimeoutPortIn;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionLifecycleSchedulerTest {

    @Mock
    private SubscriptionExpirationPortIn subscriptionExpirationPortIn;

    @Mock
    private SubscriptionPaymentTimeoutPortIn subscriptionPaymentTimeoutPortIn;

    @InjectMocks
    private SubscriptionLifecycleScheduler subscriptionLifecycleScheduler;

    @Test
    void shouldProcessBothSubscriptionLifecycleTasks() {
        when(subscriptionExpirationPortIn.expireExpiredSubscriptions()).thenReturn(2);
        when(subscriptionPaymentTimeoutPortIn.cancelExpiredPendingPayments(any(Instant.class))).thenReturn(3);

        subscriptionLifecycleScheduler.processSubscriptionLifecycle();

        verify(subscriptionExpirationPortIn).expireExpiredSubscriptions();
        verify(subscriptionPaymentTimeoutPortIn).cancelExpiredPendingPayments(any(Instant.class));
    }

    @Test
    void shouldContinueWithPaymentTimeoutWhenExpirationFails() {
        when(subscriptionExpirationPortIn.expireExpiredSubscriptions()).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(subscriptionLifecycleScheduler::processSubscriptionLifecycle);

        verify(subscriptionExpirationPortIn).expireExpiredSubscriptions();
        verify(subscriptionPaymentTimeoutPortIn).cancelExpiredPendingPayments(any(Instant.class));
    }

    @Test
    void shouldSwallowPaymentTimeoutFailureSoSchedulerKeepsRunning() {
        when(subscriptionPaymentTimeoutPortIn.cancelExpiredPendingPayments(any(Instant.class)))
                .thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(subscriptionLifecycleScheduler::processSubscriptionLifecycle);

        verify(subscriptionExpirationPortIn).expireExpiredSubscriptions();
        verify(subscriptionPaymentTimeoutPortIn).cancelExpiredPendingPayments(any(Instant.class));
    }
}
