package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

import java.util.UUID;

public record DashboardParkingLotOptionResponse(
        UUID parkingLotId,
        UUID organizationId,
        String name
) {
}
