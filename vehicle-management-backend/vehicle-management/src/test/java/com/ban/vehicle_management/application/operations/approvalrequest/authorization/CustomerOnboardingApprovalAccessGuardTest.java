package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
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
class CustomerOnboardingApprovalAccessGuardTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks
    private CustomerOnboardingApprovalAccessGuard customerOnboardingApprovalAccessGuard;

    @Test
    void shouldAllowReadWhenCurrentUserIsParkingManagerWithCustomerReadPermission() {
        CurrentAccountAccess currentAccount = currentParkingManager();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(currentAccountPortIn.hasPermission("CUSTOMER_READ_ALL")).thenReturn(true);

        CurrentAccountAccess result = customerOnboardingApprovalAccessGuard.requireReadAccess();

        assertEquals(currentAccount, result);
    }

    @Test
    void shouldAllowWriteWhenCurrentUserIsParkingManagerWithCustomerUpdatePermission() {
        CurrentAccountAccess currentAccount = currentParkingManager();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(currentAccountPortIn.hasPermission("CUSTOMER_UPDATE_ALL")).thenReturn(true);

        CurrentAccountAccess result = customerOnboardingApprovalAccessGuard.requireWriteAccess();

        assertEquals(currentAccount, result);
    }

    @Test
    void shouldRejectReadWhenCurrentUserIsSystemAdmin() {
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentSystemAdmin());

        assertThrows(AccessDeniedException.class, () -> customerOnboardingApprovalAccessGuard.requireReadAccess());
    }

    @Test
    void shouldRejectWriteWhenParkingManagerHasNoCustomerUpdatePermission() {
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentParkingManager());
        when(currentAccountPortIn.hasPermission("CUSTOMER_UPDATE_ALL")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> customerOnboardingApprovalAccessGuard.requireWriteAccess());
    }

    @Test
    void shouldAllowCurrentCustomerForCustomerOwnedFlow() {
        CurrentAccountAccess currentAccount = currentCustomer();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);

        CurrentAccountAccess result = customerOnboardingApprovalAccessGuard.requireCurrentCustomer();

        assertEquals(currentAccount, result);
    }

    private CurrentAccountAccess currentParkingManager() {
        return new CurrentAccountAccess(
                UUID.randomUUID(),
                "sub-manager",
                "parking.manager",
                "parking.manager@example.com",
                UUID.randomUUID(),
                "PARKING_MANAGER",
                AccountStatus.ACTIVE,
                null,
                Set.of("CUSTOMER_READ_ALL", "CUSTOMER_UPDATE_ALL")
        );
    }

    private CurrentAccountAccess currentSystemAdmin() {
        return new CurrentAccountAccess(
                UUID.randomUUID(),
                "sub-system-admin",
                "system.admin",
                "system.admin@example.com",
                UUID.randomUUID(),
                "SYSTEM_ADMIN",
                AccountStatus.ACTIVE,
                null,
                Set.of("CUSTOMER_READ_ALL", "CUSTOMER_UPDATE_ALL")
        );
    }

    private CurrentAccountAccess currentCustomer() {
        return new CurrentAccountAccess(
                UUID.randomUUID(),
                "sub-customer",
                "customer",
                "customer@example.com",
                UUID.randomUUID(),
                "CUSTOMER",
                AccountStatus.ACTIVE,
                null,
                Set.of()
        );
    }
}
