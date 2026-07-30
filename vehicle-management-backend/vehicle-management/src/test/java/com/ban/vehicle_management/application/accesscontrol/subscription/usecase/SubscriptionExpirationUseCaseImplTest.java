package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirationUseCaseImplTest {

    @Mock
    private SubscriptionPortOut subscriptionPortOut;

    @InjectMocks
    private SubscriptionExpirationUseCaseImpl subscriptionExpirationUseCase;

    @Test
    void shouldExpireActiveSubscriptionsBeforeBusinessDate() {
        LocalDate businessDate = LocalDate.of(2026, 7, 30);

        when(subscriptionPortOut.expireActiveSubscriptionsBefore(businessDate)).thenReturn(3);

        int expiredCount = subscriptionExpirationUseCase.expireExpiredSubscriptions(businessDate);

        assertEquals(3, expiredCount);
        verify(subscriptionPortOut).expireActiveSubscriptionsBefore(businessDate);
    }

    @Test
    void shouldRejectNullBusinessDate() {
        assertThrows(BadRequestException.class, () -> subscriptionExpirationUseCase.expireExpiredSubscriptions(null));
        verify(subscriptionPortOut, never()).expireActiveSubscriptionsBefore(null);
    }
}
