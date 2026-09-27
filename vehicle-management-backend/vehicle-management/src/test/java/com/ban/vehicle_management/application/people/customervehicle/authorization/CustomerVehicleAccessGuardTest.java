package com.ban.vehicle_management.application.people.customervehicle.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CustomerVehicleAccessGuardTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private AccountProfilePortOut accountProfilePortOut;

    @Mock
    private OrganizationAccessGuard organizationAccessGuard;

    @Mock
    private ParkingLotPortOut parkingLotPortOut;

    @Mock
    private CustomerVehiclePortOut customerVehiclePortOut;

    @InjectMocks
    private CustomerVehicleAccessGuard customerVehicleAccessGuard;

    @Test
    void shouldKeepRequestedCustomerIdWhenCreateAllPermissionIsGranted() {
        UUID requestedCustomerId = UUID.randomUUID();
        when(currentAccountPortIn.hasPermission("CUSTOMER_VEHICLE_CREATE_ALL")).thenReturn(true);
        allowPlatformScope();

        UUID resolvedCustomerId = customerVehicleAccessGuard.resolveCustomerIdForCreate(requestedCustomerId);

        assertEquals(requestedCustomerId, resolvedCustomerId);
    }

    @Test
    void shouldResolveCurrentApprovedCustomerIdWhenOnlyOwnCreatePermissionIsGranted() {
        UUID accountId = UUID.randomUUID();
        UUID currentCustomerId = UUID.randomUUID();

        when(currentAccountPortIn.hasPermission("CUSTOMER_VEHICLE_CREATE_ALL")).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(approvedCustomerProfile(accountId, currentCustomerId)));

        UUID resolvedCustomerId = customerVehicleAccessGuard.resolveCustomerIdForCreate(UUID.randomUUID());

        assertEquals(currentCustomerId, resolvedCustomerId);
        verify(currentAccountPortIn).requirePermission("CUSTOMER_VEHICLE_CREATE_OWN");
    }

    @Test
    void shouldResolveCurrentApprovedCustomerIdWhenOnlyOwnReadPermissionIsGranted() {
        UUID accountId = UUID.randomUUID();
        UUID currentCustomerId = UUID.randomUUID();

        when(currentAccountPortIn.hasPermission("CUSTOMER_VEHICLE_READ_ALL")).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(approvedCustomerProfile(accountId, currentCustomerId)));

        UUID resolvedCustomerId = customerVehicleAccessGuard.resolveCustomerIdForRead(UUID.randomUUID());

        assertEquals(currentCustomerId, resolvedCustomerId);
        verify(currentAccountPortIn).requirePermission("CUSTOMER_VEHICLE_READ_OWN");
    }

    @Test
    void shouldDenyReadingVehicleOwnedByAnotherCustomerWhenOnlyOwnReadPermissionIsGranted() {
        UUID accountId = UUID.randomUUID();
        UUID currentCustomerId = UUID.randomUUID();

        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerId(UUID.randomUUID());

        when(currentAccountPortIn.hasPermission("CUSTOMER_VEHICLE_READ_ALL")).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(approvedCustomerProfile(accountId, currentCustomerId)));

        assertThrows(AccessDeniedException.class, () -> customerVehicleAccessGuard.ensureCanRead(customerVehicle));
    }

    @Test
    void shouldDenyOwnScopeWhenCurrentCustomerIsNotApproved() {
        UUID accountId = UUID.randomUUID();
        UUID currentCustomerId = UUID.randomUUID();

        when(currentAccountPortIn.hasPermission("CUSTOMER_VEHICLE_READ_ALL")).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(pendingCustomerProfile(accountId, currentCustomerId)));

        assertThrows(
                AccessDeniedException.class,
                () -> customerVehicleAccessGuard.resolveCustomerIdForRead(UUID.randomUUID())
        );
    }

    @Test
    void shouldAllowActivatingOrInactivatingOwnVehicleWhenOnlyOwnUpdatePermissionIsGranted() {
        UUID accountId = UUID.randomUUID();
        UUID currentCustomerId = UUID.randomUUID();

        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerId(currentCustomerId);

        when(currentAccountPortIn.hasPermission("CUSTOMER_VEHICLE_UPDATE_ALL")).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(approvedCustomerProfile(accountId, currentCustomerId)));

        customerVehicleAccessGuard.ensureCanActivateOrInactivate(customerVehicle);

        verify(currentAccountPortIn).requirePermission("CUSTOMER_VEHICLE_UPDATE_OWN");
    }

    @Test
    void shouldRequireUpdateAllPermissionForBlocking() {
        allowPlatformScope();
        customerVehicleAccessGuard.ensureCanBlock();

        verify(currentAccountPortIn).requirePermission("CUSTOMER_VEHICLE_UPDATE_ALL");
    }

    @Test
    void shouldDenyPartnerReadingVehicleNotUsedAtItsLot() {
        UUID lotId = UUID.randomUUID();
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomerVehicleId(UUID.randomUUID());
        when(currentAccountPortIn.hasPermission("CUSTOMER_VEHICLE_READ_ALL")).thenReturn(true);
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(lotId));

        assertThrows(AccessDeniedException.class, () -> customerVehicleAccessGuard.ensureCanRead(vehicle));
        verify(customerVehiclePortOut).existsInParkingLots(vehicle.getCustomerVehicleId(), Set.of(lotId));
    }

    private void allowPlatformScope() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.getEffectivePermissionCodes()).thenReturn(Set.of("PARKING_SCOPE_PLATFORM"));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access);
    }

    private AccountProfileState approvedCustomerProfile(UUID accountId, UUID customerId) {
        return new AccountProfileState(
                accountId,
                "customer-user",
                "customer@example.com",
                "kc-customer-id",
                null,
                UUID.randomUUID(),
                "Customer User",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                customerId,
                "CUS-001",
                null,
                CustomerStatus.ACTIVE,
                CustomerApprovalStatus.APPROVED,
                AccountStatus.ACTIVE
        );
    }

    private AccountProfileState pendingCustomerProfile(UUID accountId, UUID customerId) {
        return new AccountProfileState(
                accountId,
                "customer-user",
                "customer@example.com",
                "kc-customer-id",
                null,
                UUID.randomUUID(),
                "Customer User",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                customerId,
                "CUS-001",
                null,
                CustomerStatus.ACTIVE,
                CustomerApprovalStatus.PENDING,
                AccountStatus.ACTIVE
        );
    }
}
