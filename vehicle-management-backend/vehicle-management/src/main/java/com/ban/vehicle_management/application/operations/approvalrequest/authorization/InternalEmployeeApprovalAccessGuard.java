package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class InternalEmployeeApprovalAccessGuard {

    public static final String REQUEST_TYPE = OnboardingApprovalPolicy.INTERNAL_EMPLOYEE_REQUEST_TYPE;
    public static final String TARGET_SCHEMA = "people";
    public static final String TARGET_TABLE = "employees";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationPortOut organizationPortOut;
    private final OnboardingApprovalPolicy onboardingApprovalPolicy = new OnboardingApprovalPolicy();

    public InternalEmployeeApprovalAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationPortOut organizationPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationPortOut = organizationPortOut;
    }

    public CurrentAccountAccess requireReadAccess() {
        CurrentAccountAccess currentAccount = requireActiveReviewer();
        if (hasAnyInternalReviewPermission()) {
            return currentAccount;
        }
        throw new AccessDeniedException("Access is denied");
    }

    public CurrentAccountAccess requireWriteAccess() {
        CurrentAccountAccess currentAccount = requireActiveReviewer();
        if (hasAnyInternalReviewPermission()) {
            return currentAccount;
        }
        throw new AccessDeniedException("Access is denied");
    }

    public void ensureCanReviewTarget(CurrentAccountAccess currentAccount, String targetRoleCode) {
        if (!canAccessTargetRole(currentAccount, targetRoleCode)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void ensureCanReviewTarget(
            CurrentAccountAccess currentAccount,
            UUID targetAccountId,
            String targetRoleCode
    ) {
        if (!canAccessTarget(currentAccount, targetAccountId, targetRoleCode)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public boolean canAccessTargetRole(CurrentAccountAccess currentAccount, String targetRoleCode) {
        return onboardingApprovalPolicy.resolveReviewerScope(currentAccount)
                .canReviewInternalTarget(targetRoleCode);
    }

    public boolean canAccessTarget(
            CurrentAccountAccess currentAccount,
            UUID targetAccountId,
            String targetRoleCode
    ) {
        if (!canAccessTargetRole(currentAccount, targetRoleCode)) {
            return false;
        }
        if (!OrganizationAccessGuard.PARTNER_ADMIN.equals(currentAccount.roleCode())
                && !OrganizationAccessGuard.PARKING_MANAGER.equals(currentAccount.roleCode())) {
            return true;
        }
        if (targetAccountId == null) {
            return false;
        }

        Set<UUID> partnerOrganizationIds = organizationPortOut.findActiveOrganizationIdsByAccountId(
                currentAccount.accountId()
        );
        if (partnerOrganizationIds.isEmpty()) {
            return false;
        }
        return organizationPortOut.findActiveOrganizationIdsByAccountId(targetAccountId).stream()
                .anyMatch(partnerOrganizationIds::contains);
    }

    private boolean hasAnyInternalReviewPermission() {
        return currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_PARKING_MANAGER_PERMISSION)
                || currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_EMPLOYEE_PERMISSION);
    }

    private CurrentAccountAccess requireActiveReviewer() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (!currentAccount.canUseBusinessPermissions()) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }
}
