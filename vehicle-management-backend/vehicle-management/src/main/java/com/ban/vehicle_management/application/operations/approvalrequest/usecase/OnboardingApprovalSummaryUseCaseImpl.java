package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.OnboardingApprovalSummaryResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.OnboardingApprovalSummaryPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.OnboardingApprovalSummaryPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy.ReviewerScope;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApprovalSummaryUseCaseImpl implements OnboardingApprovalSummaryPortIn {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OnboardingApprovalSummaryPortOut onboardingApprovalSummaryPortOut;
    private final OnboardingApprovalPolicy onboardingApprovalPolicy = new OnboardingApprovalPolicy();

    public OnboardingApprovalSummaryUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            OnboardingApprovalSummaryPortOut onboardingApprovalSummaryPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.onboardingApprovalSummaryPortOut = onboardingApprovalSummaryPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingApprovalSummaryResult getMyPendingSummary() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        ReviewerScope scope = onboardingApprovalPolicy.resolveReviewerScope(currentAccount);
        if (!scope.hasAnyReviewScope()) {
            throw new AccessDeniedException("Access is denied");
        }

        long systemAdminPending = scope.canReviewSystemAdmin()
                ? onboardingApprovalSummaryPortOut.countPendingSystemAdminApprovalsExcluding(currentAccount.accountId())
                : 0L;
        long parkingManagerPending = 0L;
        if (scope.canReviewParkingManager()) {
            parkingManagerPending = onboardingApprovalSummaryPortOut
                    .countPendingParkingManagerApprovalsCreatedBy(currentAccount.accountId());
        }
        long employeePending = 0L;
        if (scope.canReviewEmployee()) {
            employeePending = countPendingEmployeeApprovals(currentAccount);
        }
        long internalEmployeePending = parkingManagerPending + employeePending;
        long customerPending = scope.canReviewCustomer()
                ? onboardingApprovalSummaryPortOut.countPendingCustomerApprovals()
                : 0L;

        return new OnboardingApprovalSummaryResult(
                systemAdminPending + internalEmployeePending + customerPending,
                systemAdminPending,
                internalEmployeePending,
                parkingManagerPending,
                employeePending,
                customerPending
        );
    }

    private long countPendingEmployeeApprovals(CurrentAccountAccess currentAccount) {
        var permissions = currentAccount.getEffectivePermissionCodes();
        if (permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_PLATFORM)) {
            return onboardingApprovalSummaryPortOut.countPendingEmployeeLikeApprovals();
        }
        if (permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)
                && currentAccount.assignedParkingLotIds().isEmpty()) {
            return 0L;
        }
        if (!permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)
                && !permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)) {
            return 0L;
        }
        return onboardingApprovalSummaryPortOut.countPendingEmployeeLikeApprovalsInOrganizations(
                currentAccount.activeOrganizationIds());
    }
}
