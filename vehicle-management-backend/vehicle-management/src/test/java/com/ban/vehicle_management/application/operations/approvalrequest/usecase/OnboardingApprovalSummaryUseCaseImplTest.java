package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.OnboardingApprovalSummaryResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.OnboardingApprovalSummaryPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingApprovalSummaryUseCaseImplTest {

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private OnboardingApprovalSummaryPortOut onboardingApprovalSummaryPortOut;
    @InjectMocks private OnboardingApprovalSummaryUseCaseImpl useCase;

    @Test
    void getMyPendingSummary_shouldReturnOnlySystemAdminActionableScopes() {
        UUID accountId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access(accountId));
        when(onboardingApprovalSummaryPortOut.countPendingSystemAdminApprovalsExcluding(accountId)).thenReturn(2L);
        when(onboardingApprovalSummaryPortOut.countPendingParkingManagerApprovalsCreatedBy(accountId)).thenReturn(3L);

        OnboardingApprovalSummaryResult result = useCase.getMyPendingSummary();

        assertEquals(5L, result.totalPending());
        assertEquals(2L, result.systemAdminPending());
        assertEquals(3L, result.internalEmployeePending());
        assertEquals(3L, result.parkingManagerPending());
        assertEquals(0L, result.employeePending());
        assertEquals(0L, result.customerPending());
        verify(onboardingApprovalSummaryPortOut, never()).countPendingCustomerApprovals();
    }

    @Test
    void getMyPendingSummary_shouldScopeEmployeeCountToReviewerOrganizations() {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        CurrentAccountAccess currentAccount = new CurrentAccountAccess(
                accountId, "subject", "partner", "partner@example.com", UUID.randomUUID(), "PARTNER_ADMIN",
                true, true, Set.of(organizationId), Set.of(),
                AccountStatus.ACTIVE, null, null, null, false,
                Set.of(
                        OnboardingApprovalPolicy.REVIEW_EMPLOYEE_PERMISSION,
                        OrganizationAccessGuard.PARKING_SCOPE_PARTNER
                ));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(onboardingApprovalSummaryPortOut.countPendingEmployeeLikeApprovalsInOrganizations(Set.of(organizationId)))
                .thenReturn(4L);

        OnboardingApprovalSummaryResult result = useCase.getMyPendingSummary();

        assertEquals(4L, result.totalPending());
        assertEquals(0L, result.parkingManagerPending());
        assertEquals(4L, result.employeePending());
        assertEquals(4L, result.internalEmployeePending());
        verify(onboardingApprovalSummaryPortOut, never()).countPendingEmployeeLikeApprovals();
    }

    private CurrentAccountAccess access(UUID accountId) {
        return new CurrentAccountAccess(
                accountId, "subject", "admin", "admin@example.com", UUID.randomUUID(), "CUSTOM_APPROVER",
                true, true, Set.of(UUID.randomUUID()), Set.of(),
                AccountStatus.ACTIVE, null, null, null, false,
                Set.of(
                        OnboardingApprovalPolicy.REVIEW_SYSTEM_ADMIN_PERMISSION,
                        OnboardingApprovalPolicy.REVIEW_PARKING_MANAGER_PERMISSION
                ));
    }
}
