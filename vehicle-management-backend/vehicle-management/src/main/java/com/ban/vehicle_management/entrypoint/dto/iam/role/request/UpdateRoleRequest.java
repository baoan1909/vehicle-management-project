package com.ban.vehicle_management.entrypoint.dto.iam.role.request;

public record UpdateRoleRequest(
        String code,
        String name,
        String description,
        Boolean isActive
) {
}
