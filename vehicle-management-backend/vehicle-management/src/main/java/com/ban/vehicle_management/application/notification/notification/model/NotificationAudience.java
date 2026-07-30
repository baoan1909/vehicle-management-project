package com.ban.vehicle_management.application.notification.notification.model;

import java.util.Set;

public final class NotificationAudience {

    public static final Set<String> CUSTOMERS = Set.of("CUSTOMER");
    public static final Set<String> OPERATIONS = Set.of("EMPLOYEE", "PARKING_MANAGER");
    public static final Set<String> APPROVERS = Set.of("PARKING_MANAGER", "SYSTEM_ADMIN");

    private NotificationAudience() {
    }
}
