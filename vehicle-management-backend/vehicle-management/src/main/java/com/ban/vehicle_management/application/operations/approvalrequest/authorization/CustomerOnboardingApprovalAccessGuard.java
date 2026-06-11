package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class CustomerOnboardingApprovalAccessGuard {

    public static final String REQUEST_TYPE = "CUSTOMER_ONBOARDING";
    public static final String TARGET_SCHEMA = "people";
    public static final String TARGET_TABLE = "customers";

    private static final String CUSTOMER_READ_ALL = "CUSTOMER_READ_ALL";
    private static final String CUSTOMER_UPDATE_ALL = "CUSTOMER_UPDATE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;

    public CustomerOnboardingApprovalAccessGuard(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public CurrentAccountAccess requireReadAccess() {
        CurrentAccountAccess currentAccount = requireCurrentParkingManager();
        if (!currentAccountPortIn.hasPermission(CUSTOMER_READ_ALL)) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }

    public CurrentAccountAccess requireWriteAccess() {
        CurrentAccountAccess currentAccount = requireCurrentParkingManager();
        if (!currentAccountPortIn.hasPermission(CUSTOMER_UPDATE_ALL)) {
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

    private CurrentAccountAccess requireCurrentParkingManager() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (!AdminProvisionableAccountRoleCode.PARKING_MANAGER.name().equals(currentAccount.roleCode())) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }
}
