package com.ban.vehicle_management.shared.enumeration.iam;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum InternalAccountRoleCode {
    SYSTEM_ADMIN,
    PARKING_MANAGER,
    EMPLOYEE;

    public static Set<String> codes() {
        return Arrays.stream(values())
                .map(InternalAccountRoleCode::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
