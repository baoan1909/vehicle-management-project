package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import java.util.Optional;
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
    private final AccountAuthorizationPortOut accountAuthorizationPortOut;
    private final OrganizationPortOut organizationPortOut;
    private final OnboardingApprovalPolicy onboardingApprovalPolicy = new OnboardingApprovalPolicy();

    public InternalEmployeeApprovalAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            AccountAuthorizationPortOut accountAuthorizationPortOut,
            OrganizationPortOut organizationPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountAuthorizationPortOut = accountAuthorizationPortOut;
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
        if (OnboardingApprovalPolicy.PARKING_MANAGER_TARGET_ROLE.equals(targetRoleCode)
                && OrganizationAccessGuard.PARTNER_ADMIN.equals(currentAccount.roleCode())) {
            // A Partner Admin can only review the Manager account that they
            // personally provisioned. Organization membership alone is too
            // broad when an organization has more than one Partner Admin.
            return findCreatorAccountId(targetAccountId)
                    .map(currentAccount.accountId()::equals)
                    .orElse(false);
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

    /**
     * Used only to send an approval notification to the Manager's creating
     * Partner Admin. Returning an empty value intentionally prevents a broad
     * fallback broadcast for legacy accounts without creation provenance.
     */
    public Optional<UUID> findCreatorAccountId(UUID targetAccountId) {
        if (targetAccountId == null) {
            return Optional.empty();
        }
        return accountAuthorizationPortOut.findCreatedByAccountId(targetAccountId);
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
