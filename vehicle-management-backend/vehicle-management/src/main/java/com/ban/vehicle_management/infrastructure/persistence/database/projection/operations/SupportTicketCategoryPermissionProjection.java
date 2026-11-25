package com.ban.vehicle_management.infrastructure.persistence.database.projection.operations;

import java.util.UUID;

public interface SupportTicketCategoryPermissionProjection {
    UUID getCategoryId();
    String getPermissionCode();
}
