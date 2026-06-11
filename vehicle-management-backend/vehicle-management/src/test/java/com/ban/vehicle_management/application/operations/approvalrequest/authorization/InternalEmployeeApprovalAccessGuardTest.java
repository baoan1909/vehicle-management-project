package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class InternalEmployeeApprovalAccessGuardTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks
    private InternalEmployeeApprovalAccessGuard internalEmployeeApprovalAccessGuard;

    @Test
    void shouldAllowReadWhenCallerHasEmployeeReadPermission() {
        CurrentAccountAccess currentAccount = currentParkingManager(UUID.randomUUID());
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(currentAccountPortIn.hasPermission("ACCOUNT_READ_ALL")).thenReturn(false);
        when(currentAccountPortIn.hasPermission("EMPLOYEE_READ_ALL")).thenReturn(true);

        CurrentAccountAccess result = internalEmployeeApprovalAccessGuard.requireReadAccess();

        assertEquals(currentAccount, result);
    }

    @Test
    void shouldRejectWriteWhenCallerHasNoApprovalWritePermission() {
        CurrentAccountAccess currentAccount = currentEmployee(UUID.randomUUID());
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(currentAccountPortIn.hasPermission("ACCOUNT_UPDATE_ALL")).thenReturn(false);
        when(currentAccountPortIn.hasPermission("EMPLOYEE_UPDATE_ALL")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> internalEmployeeApprovalAccessGuard.requireWriteAccess());
    }

    @Test
    void shouldApplyReviewerRoleMatrix() {
        CurrentAccountAccess systemAdmin = currentSystemAdmin(UUID.randomUUID());
        CurrentAccountAccess parkingManager = currentParkingManager(UUID.randomUUID());
        CurrentAccountAccess employee = currentEmployee(UUID.randomUUID());

        assertTrue(internalEmployeeApprovalAccessGuard.canAccessTargetRole(systemAdmin, "PARKING_MANAGER"));
        assertFalse(internalEmployeeApprovalAccessGuard.canAccessTargetRole(systemAdmin, "EMPLOYEE"));
        assertTrue(internalEmployeeApprovalAccessGuard.canAccessTargetRole(parkingManager, "EMPLOYEE"));
        assertFalse(internalEmployeeApprovalAccessGuard.canAccessTargetRole(parkingManager, "PARKING_MANAGER"));
        assertFalse(internalEmployeeApprovalAccessGuard.canAccessTargetRole(employee, "EMPLOYEE"));
    }

    @Test
    void shouldRejectInvalidRoleCode() {
        assertThrows(
                AccessDeniedException.class,
                () -> internalEmployeeApprovalAccessGuard.requireProvisionableRole("SUPER_USER")
        );
    }

    @Test
    void shouldResolveValidProvisionableRole() {
        AdminProvisionableAccountRoleCode roleCode =
                internalEmployeeApprovalAccessGuard.requireProvisionableRole("EMPLOYEE");

        assertEquals(AdminProvisionableAccountRoleCode.EMPLOYEE, roleCode);
    }

    @Test
    void shouldAllowReviewWhenMatrixAllowsTarget() {
        assertDoesNotThrow(() -> internalEmployeeApprovalAccessGuard.ensureCanReviewTarget(
                currentParkingManager(UUID.randomUUID()),
                "EMPLOYEE"
        ));
    }

    private CurrentAccountAccess currentSystemAdmin(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "sub-system-admin",
                "system.admin",
                "system.admin@example.com",
                UUID.randomUUID(),
                "SYSTEM_ADMIN",
                AccountStatus.ACTIVE,
                null,
                Set.of("ACCOUNT_UPDATE_ALL", "ACCOUNT_READ_ALL")
        );
    }

    private CurrentAccountAccess currentParkingManager(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "sub-manager",
                "parking.manager",
                "parking.manager@example.com",
                UUID.randomUUID(),
                "PARKING_MANAGER",
                AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE,
                Set.of("EMPLOYEE_UPDATE_ALL", "EMPLOYEE_READ_ALL")
        );
    }

    private CurrentAccountAccess currentEmployee(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "sub-employee",
                "parking.employee",
                "parking.employee@example.com",
                UUID.randomUUID(),
                "EMPLOYEE",
                AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE,
                Set.of()
        );
    }
}
