package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
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

    @Mock
    private OrganizationPortOut organizationPortOut;

    @InjectMocks
    private InternalEmployeeApprovalAccessGuard internalEmployeeApprovalAccessGuard;

    @Test
    void shouldAllowReadWhenCallerHasEmployeeReadPermission() {
        CurrentAccountAccess currentAccount = currentParkingManager(UUID.randomUUID());
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_PARKING_MANAGER_PERMISSION))
                .thenReturn(false);
        when(currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_EMPLOYEE_PERMISSION))
                .thenReturn(true);

        CurrentAccountAccess result = internalEmployeeApprovalAccessGuard.requireReadAccess();

        assertEquals(currentAccount, result);
    }

    @Test
    void shouldRejectWriteWhenCallerHasNoApprovalWritePermission() {
        CurrentAccountAccess currentAccount = currentEmployee(UUID.randomUUID());
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_PARKING_MANAGER_PERMISSION))
                .thenReturn(false);
        when(currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_EMPLOYEE_PERMISSION))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> internalEmployeeApprovalAccessGuard.requireWriteAccess());
    }

    @Test
    void shouldAllowPartnerAdminToReviewOnlyParkingManagersInSameOrganization() {
        UUID partnerAdminAccountId = UUID.randomUUID();
        UUID ownManagerAccountId = UUID.randomUUID();
        UUID otherManagerAccountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        CurrentAccountAccess partnerAdmin = new CurrentAccountAccess(
                partnerAdminAccountId,
                "sub-partner-admin",
                "partner.admin",
                "partner.admin@example.com",
                UUID.randomUUID(),
                "PARTNER_ADMIN",
                AccountStatus.ACTIVE,
                null,
                Set.of(OnboardingApprovalPolicy.REVIEW_PARKING_MANAGER_PERMISSION)
        );
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(partnerAdminAccountId))
                .thenReturn(Set.of(organizationId));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(ownManagerAccountId))
                .thenReturn(Set.of(organizationId));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(otherManagerAccountId))
                .thenReturn(Set.of(UUID.randomUUID()));

        assertTrue(internalEmployeeApprovalAccessGuard.canAccessTarget(
                partnerAdmin,
                ownManagerAccountId,
                "PARKING_MANAGER"
        ));
        assertFalse(internalEmployeeApprovalAccessGuard.canAccessTarget(
                partnerAdmin,
                otherManagerAccountId,
                "PARKING_MANAGER"
        ));
        assertFalse(internalEmployeeApprovalAccessGuard.canAccessTarget(
                partnerAdmin,
                ownManagerAccountId,
                "EMPLOYEE"
        ));
    }

    @Test
    void partnerAdminCanReviewOwnEmployeesButNotOtherPartnersEmployees() {
        UUID partnerId = UUID.randomUUID();
        UUID ownEmployeeId = UUID.randomUUID();
        UUID otherEmployeeId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        CurrentAccountAccess partner = new CurrentAccountAccess(
                partnerId, "partner-sub", "partner", "partner@example.com", UUID.randomUUID(),
                "PARTNER_ADMIN", AccountStatus.ACTIVE, null,
                Set.of(OnboardingApprovalPolicy.REVIEW_EMPLOYEE_PERMISSION));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(partnerId))
                .thenReturn(Set.of(organizationId));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(ownEmployeeId))
                .thenReturn(Set.of(organizationId));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(otherEmployeeId))
                .thenReturn(Set.of(UUID.randomUUID()));

        assertTrue(internalEmployeeApprovalAccessGuard.canAccessTarget(partner, ownEmployeeId, "EMPLOYEE"));
        assertFalse(internalEmployeeApprovalAccessGuard.canAccessTarget(partner, otherEmployeeId, "EMPLOYEE"));
    }

    @Test
    void managerCannotReviewEmployeeFromAnotherPartner() {
        UUID managerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        CurrentAccountAccess manager = currentParkingManager(managerId);
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(managerId))
                .thenReturn(Set.of(UUID.randomUUID()));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(employeeId))
                .thenReturn(Set.of(UUID.randomUUID()));

        assertFalse(internalEmployeeApprovalAccessGuard.canAccessTarget(manager, employeeId, "EMPLOYEE"));
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
                Set.of(OnboardingApprovalPolicy.REVIEW_EMPLOYEE_PERMISSION)
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
