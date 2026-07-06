package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

import java.time.LocalDate;
import java.util.List;

public record DashboardOverviewResponse(
        LocalDate fromDate,
        LocalDate toDate,
        DashboardKpiResponse totalRevenue,
        DashboardKpiResponse checkInCount,
        DashboardKpiResponse checkOutCount,
        DashboardKpiResponse currentParkingCount,
        DashboardKpiResponse occupancyRate,
        List<RevenueTrendPointResponse> revenueTrend,
        VehicleTypeRatioResponse vehicleTypeRatio,
        CardStatusOverviewResponse cardStatus,
        UserGrowthOverviewResponse userGrowth,
        List<DeviceStatusItemResponse> deviceStatus
) {
}