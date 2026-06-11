package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SystemAdminApprovalAccessGuard {

    public static final String REQUEST_TYPE = "SYSTEM_ADMIN_ONBOARDING";
    public static final String TARGET_SCHEMA = "iam";
    public static final String TARGET_TABLE = "accounts";

    private static final String ACCOUNT_READ_ALL = "ACCOUNT_READ_ALL";
    private static final String ACCOUNT_UPDATE_ALL = "ACCOUNT_UPDATE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;

    public SystemAdminApprovalAccessGuard(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public CurrentAccountAccess requireReadAccess() {
        CurrentAccountAccess currentAccount = requireCurrentSystemAdmin();
        if (!currentAccountPortIn.hasPermission(ACCOUNT_READ_ALL)) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }

    public CurrentAccountAccess requireWriteAccess() {
        CurrentAccountAccess currentAccount = requireCurrentSystemAdmin();
        if (!currentAccountPortIn.hasPermission(ACCOUNT_UPDATE_ALL)) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }

    public CurrentAccountAccess requireCurrentSystemAdmin() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (!AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.name().equals(currentAccount.roleCode())) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }

    public void ensureNotSelfReview(UUID approverAccountId, UUID targetAccountId) {
        if (approverAccountId.equals(targetAccountId)) {
            throw new ConflictException("System admin approval request cannot be self-reviewed");
        }
    }
}
