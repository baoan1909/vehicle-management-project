package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;

import java.util.Optional;
import java.util.UUID;

public interface CurrentAccountPortIn {

    Optional<CurrentAccountAccess> getCurrentAccount();

    CurrentAccountAccess getCurrentAccountOrThrow();

    Optional<UUID> getCurrentAccountId();

    UUID getCurrentAccountIdOrThrow();

    boolean hasPermission(String permissionCode);

    void requirePermission(String permissionCode);
}
