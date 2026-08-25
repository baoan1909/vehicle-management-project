package com.ban.vehicle_management.domain.operations.approvalrequest.policy;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Set;
import java.util.UUID;

public class OnboardingApprovalPolicy {

    public static final String SYSTEM_ADMIN_REQUEST_TYPE = "SYSTEM_ADMIN_ONBOARDING";
    public static final String INTERNAL_EMPLOYEE_REQUEST_TYPE = "INTERNAL_EMPLOYEE_ONBOARDING";
    public static final String CUSTOMER_REQUEST_TYPE = "CUSTOMER_ONBOARDING";
    public static final String REVIEW_SYSTEM_ADMIN_PERMISSION =
            "ONBOARDING_APPROVAL_REVIEW_SYSTEM_ADMIN_ALL";
    public static final String REVIEW_PARKING_MANAGER_PERMISSION =
            "ONBOARDING_APPROVAL_REVIEW_PARKING_MANAGER_ALL";
    public static final String REVIEW_EMPLOYEE_PERMISSION =
            "ONBOARDING_APPROVAL_REVIEW_EMPLOYEE_ALL";
    public static final String REVIEW_CUSTOMER_PERMISSION =
            "ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL";
    public static final String PARKING_MANAGER_TARGET_ROLE = "PARKING_MANAGER";

    public ReviewerAudience resolveReviewerAudience(String requestType, String targetRoleCode, UUID requestedBy) {
        if (SYSTEM_ADMIN_REQUEST_TYPE.equals(requestType)) {
            return audience(OnboardingApprovalKind.SYSTEM_ADMIN,
                    REVIEW_SYSTEM_ADMIN_PERMISSION, requestedBy);
        }
        if (CUSTOMER_REQUEST_TYPE.equals(requestType)) {
            return audience(OnboardingApprovalKind.CUSTOMER,
                    REVIEW_CUSTOMER_PERMISSION, requestedBy);
        }
        if (INTERNAL_EMPLOYEE_REQUEST_TYPE.equals(requestType)) {
            return audience(OnboardingApprovalKind.INTERNAL_EMPLOYEE,
                    requiredInternalReviewPermission(targetRoleCode), requestedBy);
        }
        throw new BadRequestException("Unsupported onboarding approval request type");
    }

    public ReviewerScope resolveReviewerScope(CurrentAccountAccess currentAccount) {
        if (currentAccount == null || !currentAccount.canUseBusinessPermissions()) {
            return ReviewerScope.none();
        }
        Set<String> permissions = currentAccount.getEffectivePermissionCodes();
        return new ReviewerScope(
                permissions.contains(REVIEW_SYSTEM_ADMIN_PERMISSION),
                permissions.contains(REVIEW_PARKING_MANAGER_PERMISSION),
                permissions.contains(REVIEW_EMPLOYEE_PERMISSION),
                permissions.contains(REVIEW_CUSTOMER_PERMISSION)
        );
    }

    private ReviewerAudience audience(
            OnboardingApprovalKind kind,
            String requiredPermission,
            UUID requestedBy
    ) {
        return new ReviewerAudience(kind, Set.of(requiredPermission),
                requestedBy == null ? Set.of() : Set.of(requestedBy));
    }

    private String requiredInternalReviewPermission(String targetRoleCode) {
        if (targetRoleCode == null || targetRoleCode.isBlank()) {
            throw new BadRequestException("targetRoleCode must not be blank for internal onboarding");
        }
        return PARKING_MANAGER_TARGET_ROLE.equals(targetRoleCode)
                ? REVIEW_PARKING_MANAGER_PERMISSION
                : REVIEW_EMPLOYEE_PERMISSION;
    }

    public enum OnboardingApprovalKind { SYSTEM_ADMIN, INTERNAL_EMPLOYEE, CUSTOMER }

    public record ReviewerAudience(
            OnboardingApprovalKind kind,
            Set<String> requiredPermissionCodes,
            Set<UUID> excludedAccountIds
    ) {
    }

    public record ReviewerScope(
            boolean canReviewSystemAdmin,
            boolean canReviewParkingManager,
            boolean canReviewEmployee,
            boolean canReviewCustomer
    ) {
        public static ReviewerScope none() {
            return new ReviewerScope(false, false, false, false);
        }

        public boolean canReviewInternalEmployee() {
            return canReviewParkingManager || canReviewEmployee;
        }

        public boolean canReviewInternalTarget(String targetRoleCode) {
            return PARKING_MANAGER_TARGET_ROLE.equals(targetRoleCode)
                    ? canReviewParkingManager
                    : canReviewEmployee;
        }

        public boolean hasAnyReviewScope() {
            return canReviewSystemAdmin || canReviewInternalEmployee() || canReviewCustomer;
        }
    }
}
