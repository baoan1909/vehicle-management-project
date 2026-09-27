package com.ban.vehicle_management.application.people.customer.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CustomerAccessGuardTest {

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private OrganizationAccessGuard organizationAccessGuard;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private CustomerPortOut customerPortOut;
    @Mock private CurrentAccountAccess currentAccount;
    @InjectMocks private CustomerAccessGuard guard;

    @Test
    void partnerReadIsLimitedToItsParkingLots() {
        UUID lotId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(lotId));

        assertEquals(Set.of(lotId), guard.visibleParkingLotIds());
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanRead(customerId));
        verify(customerPortOut).existsInParkingLots(customerId, Set.of(lotId));
    }

    @Test
    void requestedLotMustBelongToAccessibleScope() {
        UUID allowedLotId = UUID.randomUUID();
        UUID otherLotId = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(allowedLotId));

        assertEquals(Set.of(allowedLotId), guard.visibleParkingLotIds(allowedLotId));
        assertThrows(AccessDeniedException.class, () -> guard.visibleParkingLotIds(otherLotId));
    }

    @Test
    void platformReadDoesNotRequireLotRelationship() {
        UUID customerId = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(null);
        guard.ensureCanRead(customerId);
        verify(currentAccountPortIn).requirePermission("CUSTOMER_READ_ALL");
    }

    @Test
    void partnerCannotModifyPlatformCustomerProfile() {
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(currentAccount.getEffectivePermissionCodes()).thenReturn(Set.of("PARKING_SCOPE_PARTNER"));

        assertThrows(AccessDeniedException.class, guard::ensureCanManage);
        verify(currentAccountPortIn).requirePermission("CUSTOMER_UPDATE_ALL");
    }
}
