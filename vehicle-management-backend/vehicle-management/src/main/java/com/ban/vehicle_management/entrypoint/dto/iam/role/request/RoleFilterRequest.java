package com.ban.vehicle_management.entrypoint.dto.iam.role.request;

public record RoleFilterRequest(
        Boolean isActive,
        Boolean isSystem,
        String keyword
) {
}
