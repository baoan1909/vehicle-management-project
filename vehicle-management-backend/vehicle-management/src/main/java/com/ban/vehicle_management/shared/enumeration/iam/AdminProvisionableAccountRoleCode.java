package com.ban.vehicle_management.shared.enumeration.iam;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum AdminProvisionableAccountRoleCode {
    CUSTOMER,
    SYSTEM_ADMIN,
    PARKING_MANAGER,
    EMPLOYEE;

    public static Set<String> codes() {
        return Arrays.stream(values())
                .map(AdminProvisionableAccountRoleCode::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isInternalRole() {
        return switch (this) {
            case SYSTEM_ADMIN, PARKING_MANAGER, EMPLOYEE -> true;
            case CUSTOMER -> false;
        };
    }

    public boolean requiresEmployeeRecord() {
        return switch (this) {
            case PARKING_MANAGER, EMPLOYEE -> true;
            case CUSTOMER, SYSTEM_ADMIN -> false;
        };
    }
}
