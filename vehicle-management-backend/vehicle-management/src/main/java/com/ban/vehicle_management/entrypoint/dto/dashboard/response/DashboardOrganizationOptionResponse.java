package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

import java.util.UUID;

public record DashboardOrganizationOptionResponse(
        UUID organizationId,
        String name
) {
}
