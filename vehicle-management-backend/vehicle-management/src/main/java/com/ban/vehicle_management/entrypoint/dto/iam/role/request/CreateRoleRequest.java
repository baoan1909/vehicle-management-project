package com.ban.vehicle_management.entrypoint.dto.iam.role.request;

public record CreateRoleRequest(
        String code,
        String name,
        String description
) {
}
