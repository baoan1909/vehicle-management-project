package com.ban.vehicle_management.infrastructure.scheduler.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionExpirationPortIn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirationSchedulerTest {

    @Mock
    private SubscriptionExpirationPortIn subscriptionExpirationPortIn;

    @InjectMocks
    private SubscriptionExpirationScheduler subscriptionExpirationScheduler;

    @Test
    void shouldCallExpirationUseCase() {
        when(subscriptionExpirationPortIn.expireExpiredSubscriptions()).thenReturn(2);

        subscriptionExpirationScheduler.expireExpiredSubscriptions();

        verify(subscriptionExpirationPortIn).expireExpiredSubscriptions();
    }

    @Test
    void shouldSwallowUseCaseFailureSoSchedulerKeepsRunning() {
        when(subscriptionExpirationPortIn.expireExpiredSubscriptions()).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> subscriptionExpirationScheduler.expireExpiredSubscriptions());

        verify(subscriptionExpirationPortIn).expireExpiredSubscriptions();
    }
}
