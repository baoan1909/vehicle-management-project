package com.ban.vehicle_management.domain.operations.approvalrequest.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingApprovalPolicyTest {

    private final OnboardingApprovalPolicy policy = new OnboardingApprovalPolicy();

    @Test
    void resolveReviewerAudience_shouldRequireParkingManagerPermissionForParkingManagerTarget() {
        UUID requesterId = UUID.randomUUID();
        OnboardingApprovalPolicy.ReviewerAudience audience = policy.resolveReviewerAudience(
                OnboardingApprovalPolicy.INTERNAL_EMPLOYEE_REQUEST_TYPE,
                "PARKING_MANAGER",
                requesterId
        );

        assertEquals(Set.of(OnboardingApprovalPolicy.REVIEW_PARKING_MANAGER_PERMISSION),
                audience.requiredPermissionCodes());
        assertEquals(Set.of(requesterId), audience.excludedAccountIds());
    }

    @Test
    void resolveReviewerAudience_shouldTreatCustomInternalRoleAsEmployeeLike() {
        OnboardingApprovalPolicy.ReviewerAudience audience = policy.resolveReviewerAudience(
                OnboardingApprovalPolicy.INTERNAL_EMPLOYEE_REQUEST_TYPE,
                "CUSTOM_OPERATIONS",
                UUID.randomUUID()
        );

        assertEquals(Set.of(OnboardingApprovalPolicy.REVIEW_EMPLOYEE_PERMISSION),
                audience.requiredPermissionCodes());
    }

    @Test
    void resolveReviewerScope_shouldRejectPendingCustomReviewer() {
        CurrentAccountAccess access = access(
                "CUSTOM_APPROVER",
                EmployeeStatus.INACTIVE,
                Set.of(OnboardingApprovalPolicy.REVIEW_EMPLOYEE_PERMISSION)
        );

        assertFalse(policy.resolveReviewerScope(access).hasAnyReviewScope());
    }

    @Test
    void resolveReviewerScope_shouldExposeOnlySystemAdminMatrixPermissions() {
        CurrentAccountAccess access = access(
                "CUSTOM_HIGH_LEVEL_REVIEWER",
                null,
                Set.of(
                        OnboardingApprovalPolicy.REVIEW_SYSTEM_ADMIN_PERMISSION,
                        OnboardingApprovalPolicy.REVIEW_PARKING_MANAGER_PERMISSION
                )
        );

        OnboardingApprovalPolicy.ReviewerScope scope = policy.resolveReviewerScope(access);

        assertTrue(scope.canReviewSystemAdmin());
        assertTrue(scope.canReviewParkingManager());
        assertFalse(scope.canReviewEmployee());
        assertFalse(scope.canReviewCustomer());
    }

    @Test
    void resolveReviewerScope_shouldExposeOnlyParkingManagerMatrixPermissions() {
        CurrentAccountAccess access = access(
                "CUSTOM_OPERATIONS_REVIEWER",
                EmployeeStatus.ACTIVE,
                Set.of(
                        OnboardingApprovalPolicy.REVIEW_EMPLOYEE_PERMISSION,
                        OnboardingApprovalPolicy.REVIEW_CUSTOMER_PERMISSION
                )
        );

        OnboardingApprovalPolicy.ReviewerScope scope = policy.resolveReviewerScope(access);

        assertFalse(scope.canReviewSystemAdmin());
        assertFalse(scope.canReviewParkingManager());
        assertTrue(scope.canReviewEmployee());
        assertTrue(scope.canReviewCustomer());
    }

    @Test
    void reviewerScope_shouldMatchInternalTargetUsingDedicatedPermission() {
        OnboardingApprovalPolicy.ReviewerScope scope = new OnboardingApprovalPolicy.ReviewerScope(
                false, true, false, false
        );

        assertTrue(scope.canReviewInternalTarget("PARKING_MANAGER"));
        assertFalse(scope.canReviewInternalTarget("EMPLOYEE"));
        assertFalse(scope.canReviewInternalTarget("CUSTOM_OPERATIONS"));
    }

    @Test
    void resolveReviewerAudience_shouldExposeCustomerPermission() {
        OnboardingApprovalPolicy.ReviewerAudience audience = policy.resolveReviewerAudience(
                OnboardingApprovalPolicy.CUSTOMER_REQUEST_TYPE,
                null,
                UUID.randomUUID()
        );

        assertEquals(Set.of(OnboardingApprovalPolicy.REVIEW_CUSTOMER_PERMISSION),
                audience.requiredPermissionCodes());
    }

    @Test
    void resolveReviewerAudience_shouldRejectUnsupportedRequestType() {
        assertThrows(BadRequestException.class,
                () -> policy.resolveReviewerAudience("UNKNOWN_ONBOARDING", null, UUID.randomUUID()));
    }

    private CurrentAccountAccess access(String roleCode, EmployeeStatus employeeStatus, Set<String> permissions) {
        return new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "reviewer", "reviewer@example.com", UUID.randomUUID(),
                roleCode, AccountStatus.ACTIVE, employeeStatus, null, null, permissions);
    }
}
