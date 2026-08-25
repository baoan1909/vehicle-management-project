package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class CustomerOnboardingApprovalAccessGuard {

    public static final String REQUEST_TYPE = OnboardingApprovalPolicy.CUSTOMER_REQUEST_TYPE;
    public static final String TARGET_SCHEMA = "people";
    public static final String TARGET_TABLE = "customers";

    private final CurrentAccountPortIn currentAccountPortIn;

    public CustomerOnboardingApprovalAccessGuard(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public CurrentAccountAccess requireReadAccess() {
        CurrentAccountAccess currentAccount = requireActiveReviewer();
        if (!currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_CUSTOMER_PERMISSION)) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }

    public CurrentAccountAccess requireWriteAccess() {
        CurrentAccountAccess currentAccount = requireActiveReviewer();
        if (!currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_CUSTOMER_PERMISSION)) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }

    public CurrentAccountAccess requireCurrentCustomer() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (!AdminProvisionableAccountRoleCode.CUSTOMER.name().equals(currentAccount.roleCode())) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }

    private CurrentAccountAccess requireActiveReviewer() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (!currentAccount.canUseBusinessPermissions()) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }
}
