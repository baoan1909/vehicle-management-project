package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.parking.allocation.SubscriptionParkingAllocationService;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerAccountDeletionFinalizerTest {

    private static final Instant COMPLETED_AT = Instant.parse("2026-11-20T00:00:00Z");
    private final UUID accountId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();
    private AccountDeletionPortOut accountDeletionPort;
    private SubscriptionPortOut subscriptionPort;
    private SubscriptionParkingAllocationService allocationService;
    private CustomerAccountDeletionFinalizer finalizer;

    @BeforeEach
    void setUp() {
        accountDeletionPort = mock(AccountDeletionPortOut.class);
        subscriptionPort = mock(SubscriptionPortOut.class);
        allocationService = mock(SubscriptionParkingAllocationService.class);
        finalizer = new CustomerAccountDeletionFinalizer(
                accountDeletionPort, subscriptionPort, mock(CardPortOut.class), allocationService);
        when(accountDeletionPort.findDeletionCustomerId(accountId)).thenReturn(Optional.of(customerId));
    }

    @Test
    void terminatesActiveSubscriptionAfterExplicitNoRefundConsent() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionPort.findAll(customerId, null, null, null,
                SubscriptionStatus.ACTIVE, null, null, null)).thenReturn(List.of(subscription));
        when(subscriptionPort.findByIdForUpdate(subscription.getSubscriptionId()))
                .thenReturn(Optional.of(subscription));

        finalizer.terminateActiveSubscriptions(request(true), COMPLETED_AT);

        verify(allocationService).release(subscription.getSubscriptionId(), true);
        verify(subscriptionPort).save(subscription);
        assertEquals(SubscriptionStatus.TERMINATED, subscription.getStatus());
        assertEquals(COMPLETED_AT, subscription.getTerminatedAt());
        assertEquals("ACCOUNT_DELETION", subscription.getTerminationReason());
        assertEquals(requestId, subscription.getAccountDeletionRequestId());
    }

    @Test
    void refusesTerminationWhenPersistedConsentIsMissing() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionPort.findAll(customerId, null, null, null,
                SubscriptionStatus.ACTIVE, null, null, null)).thenReturn(List.of(subscription));

        assertThrows(IllegalStateException.class,
                () -> finalizer.terminateActiveSubscriptions(request(false), COMPLETED_AT));

        verify(allocationService, never()).release(any(), any(Boolean.class));
        verify(subscriptionPort, never()).save(any());
    }

    private AccountDeletionRequest request(boolean noRefundConfirmed) {
        return new AccountDeletionRequest(
                requestId,
                accountId,
                AccountDeletionRequestStatus.PROCESSING,
                COMPLETED_AT.minusSeconds(1_209_600),
                COMPLETED_AT,
                null,
                null,
                1,
                null,
                AccountStatus.ACTIVE,
                noRefundConfirmed);
    }
}
