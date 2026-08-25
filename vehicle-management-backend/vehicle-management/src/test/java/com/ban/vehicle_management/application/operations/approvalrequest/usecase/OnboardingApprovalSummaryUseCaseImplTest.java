package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
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
        when(onboardingApprovalSummaryPortOut.countPendingParkingManagerApprovals()).thenReturn(3L);

        OnboardingApprovalSummaryResult result = useCase.getMyPendingSummary();

        assertEquals(5L, result.totalPending());
        assertEquals(2L, result.systemAdminPending());
        assertEquals(3L, result.internalEmployeePending());
        assertEquals(0L, result.customerPending());
        verify(onboardingApprovalSummaryPortOut, never()).countPendingCustomerApprovals();
    }

    private CurrentAccountAccess access(UUID accountId) {
        return new CurrentAccountAccess(
                accountId, "subject", "admin", "admin@example.com", UUID.randomUUID(), "CUSTOM_APPROVER",
                AccountStatus.ACTIVE, null, null, null,
                Set.of(
                        OnboardingApprovalPolicy.REVIEW_SYSTEM_ADMIN_PERMISSION,
                        OnboardingApprovalPolicy.REVIEW_PARKING_MANAGER_PERMISSION
                ));
    }
}
