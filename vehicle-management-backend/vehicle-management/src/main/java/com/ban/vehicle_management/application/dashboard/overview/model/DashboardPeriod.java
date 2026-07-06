package com.ban.vehicle_management.application.dashboard.overview.model;

import java.time.Instant;
import java.time.LocalDate;

public record DashboardPeriod(
        LocalDate fromDate,
        LocalDate toDate,
        Instant fromInstant,
        Instant toInstantExclusive,
        LocalDate previousFromDate,
        LocalDate previousToDate,
        Instant previousFromInstant,
        Instant previousToInstantExclusive
) {
}