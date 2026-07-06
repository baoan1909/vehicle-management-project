package com.ban.vehicle_management.entrypoint.dto.dashboard.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record DashboardOverviewFilterRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate
) {
}