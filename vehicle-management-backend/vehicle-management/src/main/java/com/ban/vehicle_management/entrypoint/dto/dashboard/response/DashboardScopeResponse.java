package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

import java.util.List;
import java.util.UUID;

public record DashboardScopeResponse(
        String level,
        String title,
        UUID organizationId,
        UUID parkingLotId,
        List<DashboardOrganizationOptionResponse> organizations,
        List<DashboardParkingLotOptionResponse> parkingLots
) {
}
