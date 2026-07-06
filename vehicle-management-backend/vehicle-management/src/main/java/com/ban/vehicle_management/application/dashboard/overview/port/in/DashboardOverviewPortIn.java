package com.ban.vehicle_management.application.dashboard.overview.port.in;

import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardOverviewResponse;

import java.time.LocalDate;

public interface DashboardOverviewPortIn {
    DashboardOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate);
}