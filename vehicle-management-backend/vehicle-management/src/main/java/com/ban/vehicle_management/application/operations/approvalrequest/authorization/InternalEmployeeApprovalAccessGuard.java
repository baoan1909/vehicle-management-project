package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class InternalEmployeeApprovalAccessGuard {

    public static final String REQUEST_TYPE = "INTERNAL_EMPLOYEE_ONBOARDING";
    public static final String TARGET_SCHEMA = "people";
    public static final String TARGET_TABLE = "employees";

    private static final String ACCOUNT_READ_ALL = "ACCOUNT_READ_ALL";
    private static final String ACCOUNT_UPDATE_ALL = "ACCOUNT_UPDATE_ALL";
    private static final String EMPLOYEE_READ_ALL = "EMPLOYEE_READ_ALL";
    private static final String EMPLOYEE_UPDATE_ALL = "EMPLOYEE_UPDATE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;

    public InternalEmployeeApprovalAccessGuard(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public CurrentAccountAccess requireReadAccess() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (currentAccountPortIn.hasPermission(ACCOUNT_READ_ALL)
                || currentAccountPortIn.hasPermission(EMPLOYEE_READ_ALL)) {
            return currentAccount;
        }
        throw new AccessDeniedException("Access is denied");
    }

    public CurrentAccountAccess requireWriteAccess() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (currentAccountPortIn.hasPermission(ACCOUNT_UPDATE_ALL)
                || currentAccountPortIn.hasPermission(EMPLOYEE_UPDATE_ALL)) {
            return currentAccount;
        }
        throw new AccessDeniedException("Access is denied");
    }

    public void ensureCanReviewTarget(CurrentAccountAccess currentAccount, String targetRoleCode) {
        if (!canAccessTargetRole(currentAccount, targetRoleCode)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public boolean canAccessTargetRole(CurrentAccountAccess currentAccount, String targetRoleCode) {
        AdminProvisionableAccountRoleCode approverRole = requireProvisionableRole(currentAccount.roleCode());
        AdminProvisionableAccountRoleCode targetRole = requireProvisionableRole(targetRoleCode);
        if (!targetRole.requiresEmployeeRecord()) {
            return false;
        }
        return switch (approverRole) {
            case SYSTEM_ADMIN -> AdminProvisionableAccountRoleCode.PARKING_MANAGER.equals(targetRole);
            case PARKING_MANAGER -> AdminProvisionableAccountRoleCode.EMPLOYEE.equals(targetRole);
            case CUSTOMER, EMPLOYEE -> false;
        };
    }

    public AdminProvisionableAccountRoleCode requireProvisionableRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new AccessDeniedException("Access is denied");
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Access is denied");
        }
    }
}
