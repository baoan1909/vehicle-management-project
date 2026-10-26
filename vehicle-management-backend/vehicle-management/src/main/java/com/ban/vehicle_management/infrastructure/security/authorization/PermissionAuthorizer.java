package com.ban.vehicle_management.infrastructure.security.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component("permissionAuthorizer")
public class PermissionAuthorizer {

    private final CurrentAccountPortIn currentAccountPortIn;

    public PermissionAuthorizer(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public boolean hasPermission(String permissionCode) {
        return currentAccountPortIn.hasPermission(permissionCode);
    }

    public boolean hasAnyPermission(String... permissionCodes) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccount().orElse(null);
        if (currentAccount == null || !currentAccount.canUseEffectivePermissions()) {
            return false;
        }
        Set<String> effectivePermissions = currentAccount.getEffectivePermissionCodes();
        return Arrays.stream(permissionCodes == null ? new String[0] : permissionCodes)
                .filter(permissionCode -> permissionCode != null && !permissionCode.isBlank())
                .map(permissionCode -> TextValidationUtils.normalizeCode(
                        permissionCode,
                        "permissionCode",
                        100
                ))
                .anyMatch(effectivePermissions::contains);
    }

    public void requirePermission(String permissionCode) {
        currentAccountPortIn.requirePermission(permissionCode);
    }
}
