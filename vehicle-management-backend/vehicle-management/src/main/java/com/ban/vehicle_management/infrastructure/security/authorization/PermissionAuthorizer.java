package com.ban.vehicle_management.infrastructure.security.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import org.springframework.stereotype.Component;

@Component("permissionAuthorizer")
public class PermissionAuthorizer {

    private final CurrentAccountPortIn currentAccountPortIn;

    public PermissionAuthorizer(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public boolean hasPermission(String permissionCode) {
        return currentAccountPortIn.hasPermission(permissionCode);
    }

    public void requirePermission(String permissionCode) {
        currentAccountPortIn.requirePermission(permissionCode);
    }
}
