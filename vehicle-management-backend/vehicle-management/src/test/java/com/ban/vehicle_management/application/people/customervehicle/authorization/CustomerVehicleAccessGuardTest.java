package com.ban.vehicle_management.application.people.customervehicle.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CustomerVehicleAccessGuardTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private AccountProfilePortOut accountProfilePortOut;

    @InjectMocks
    private CustomerVehicleAccessGuard customerVehicleAccessGuard;

    @Test
    void shouldKeepRequestedCustomerIdWhenCreateAllPermissionIsGranted() {
        UUID requestedCustomerId = UUID.randomUUID();
        when(currentAccountPortIn.hasPermission("CUSTOMER_VEHICLE_CREATE_ALL")).thenReturn(true);

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
        customerVehicleAccessGuard.ensureCanBlock();

        verify(currentAccountPortIn).requirePermission("CUSTOMER_VEHICLE_UPDATE_ALL");
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
