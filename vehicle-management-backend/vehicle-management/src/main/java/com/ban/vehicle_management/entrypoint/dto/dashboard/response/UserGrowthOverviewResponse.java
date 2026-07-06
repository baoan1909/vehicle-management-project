package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

public record UserGrowthOverviewResponse(
        DashboardKpiResponse newAccountCount,
        DashboardKpiResponse newCustomerCount,
        DashboardKpiResponse newCustomerVehicleCount
) {
}