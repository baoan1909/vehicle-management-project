package com.ban.vehicle_management.infrastructure.security;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPort;
import org.springframework.stereotype.Component;

@Component("permissionAuthorizer")
public class PermissionAuthorizer {

    private final CurrentAccountPort currentAccountPort;

    public PermissionAuthorizer(CurrentAccountPort currentAccountPort) {
        this.currentAccountPort = currentAccountPort;
    }

    public boolean hasPermission(String permissionCode) {
        return currentAccountPort.hasPermission(permissionCode);
    }

    public void requirePermission(String permissionCode) {
        currentAccountPort.requirePermission(permissionCode);
    }
}
