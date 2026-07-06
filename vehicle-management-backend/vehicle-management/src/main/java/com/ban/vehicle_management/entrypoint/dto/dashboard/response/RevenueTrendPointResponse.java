package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueTrendPointResponse(
        LocalDate date,
        BigDecimal value
) {
}