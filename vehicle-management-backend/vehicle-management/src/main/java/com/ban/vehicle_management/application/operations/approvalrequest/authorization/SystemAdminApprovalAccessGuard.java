package com.ban.vehicle_management.application.operations.approvalrequest.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SystemAdminApprovalAccessGuard {

    public static final String REQUEST_TYPE = OnboardingApprovalPolicy.SYSTEM_ADMIN_REQUEST_TYPE;
    public static final String TARGET_SCHEMA = "iam";
    public static final String TARGET_TABLE = "accounts";

    private final CurrentAccountPortIn currentAccountPortIn;

    public SystemAdminApprovalAccessGuard(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public CurrentAccountAccess requireReadAccess() {
        CurrentAccountAccess currentAccount = requireActiveReviewer();
        if (!currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_SYSTEM_ADMIN_PERMISSION)) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccount;
    }

    public CurrentAccountAccess requireWriteAccess() {
        CurrentAccountAccess currentAccount = requireActiveReviewer();
        if (!currentAccountPortIn.hasPermission(OnboardingApprovalPolicy.REVIEW_SYSTEM_ADMIN_PERMISSION)) {
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

    public void ensureNotSelfReview(UUID approverAccountId, UUID targetAccountId) {
        if (approverAccountId.equals(targetAccountId)) {
            throw new ConflictException("System admin approval request cannot be self-reviewed");
        }
    }
}
