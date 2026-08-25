package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SystemAdminApprovalAccessGuardTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks
    private SystemAdminApprovalAccessGuard systemAdminApprovalAccessGuard;

    @Test
    void shouldAllowReadWhenCurrentUserIsSystemAdminWithAccountReadPermission() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess currentAccount = currentSystemAdmin(accountId);

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_SYSTEM_ADMIN_PERMISSION))
                .thenReturn(true);

        CurrentAccountAccess result = systemAdminApprovalAccessGuard.requireReadAccess();

        assertEquals(currentAccount, result);
    }

    @Test
    void shouldAllowReadForCustomRoleWithPermission() {
        CurrentAccountAccess currentAccount = currentCustomReviewer(UUID.randomUUID());
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_SYSTEM_ADMIN_PERMISSION))
                .thenReturn(true);

        assertEquals(currentAccount, systemAdminApprovalAccessGuard.requireReadAccess());
    }

    @Test
    void shouldRejectWriteWhenCurrentSystemAdminHasNoAccountUpdatePermission() {
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentSystemAdmin(UUID.randomUUID()));
        when(currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_SYSTEM_ADMIN_PERMISSION))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> systemAdminApprovalAccessGuard.requireWriteAccess());
    }

    @Test
    void shouldRejectSelfReview() {
        UUID accountId = UUID.randomUUID();

        assertThrows(
                ConflictException.class,
                () -> systemAdminApprovalAccessGuard.ensureNotSelfReview(accountId, accountId)
        );
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
                Set.of(OnboardingApprovalPolicy.REVIEW_SYSTEM_ADMIN_PERMISSION)
        );
    }

    private CurrentAccountAccess currentCustomReviewer(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "sub-manager",
                "parking.manager",
                "parking.manager@example.com",
                UUID.randomUUID(),
                "CUSTOM_APPROVER",
                AccountStatus.ACTIVE,
                null,
                Set.of(OnboardingApprovalPolicy.REVIEW_SYSTEM_ADMIN_PERMISSION)
        );
    }
}
