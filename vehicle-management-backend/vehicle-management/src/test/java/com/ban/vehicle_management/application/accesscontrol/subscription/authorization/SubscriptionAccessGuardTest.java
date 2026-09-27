package com.ban.vehicle_management.application.accesscontrol.subscription.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SubscriptionAccessGuardTest {
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock AccountProfilePortOut accountProfilePortOut;
    @Mock OrganizationAccessGuard organizationAccessGuard;
    @Mock ParkingLotPortOut parkingLotPortOut;
    @InjectMocks SubscriptionAccessGuard guard;

    @Test
    void partnerCanReadAndOperateOnlyOwnedLotEvenWithAllOperationPermissions() {
        UUID ownLot = UUID.randomUUID();
        UUID otherLot = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(Set.of(
                OrganizationAccessGuard.PARKING_SCOPE_PARTNER)));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(ownLot));

        assertDoesNotThrow(() -> guard.ensureCanCreateAtParkingLot(ownLot));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanCreateAtParkingLot(otherLot));
        assertDoesNotThrow(() -> guard.ensureCanOperate(subscription(ownLot)));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanOperate(subscription(otherLot)));
    }

    @Test
    void assignedManagerCannotOperateAnotherLot() {
        UUID assignedLot = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(Set.of(
                OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(assignedLot));

        assertDoesNotThrow(() -> guard.ensureCanOperate(subscription(assignedLot)));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanOperate(subscription(UUID.randomUUID())));
    }

    @Test
    void platformScopeCanReadButCannotOperateSubscription() {
        UUID lotId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(Set.of(
                OrganizationAccessGuard.PARKING_SCOPE_PLATFORM)));
        when(currentAccountPortIn.hasPermission(SubscriptionAccessGuard.READ_ALL)).thenReturn(true);
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(null);

        assertDoesNotThrow(() -> guard.ensureCanRead(subscription(lotId)));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanOperate(subscription(lotId)));
    }

    private CurrentAccountAccess account(Set<String> permissions) {
        return new CurrentAccountAccess(UUID.randomUUID(), "subject", "user", "user@example.com",
                UUID.randomUUID(), "TEST_ROLE", AccountStatus.ACTIVE, null, permissions);
    }

    private Subscription subscription(UUID lotId) {
        Subscription subscription = new Subscription();
        subscription.setParkingLotId(lotId);
        return subscription;
    }
}
