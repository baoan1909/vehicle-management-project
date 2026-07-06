package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

import java.math.BigDecimal;

public record DashboardKpiResponse(
        BigDecimal value,
        BigDecimal previousValue,
        BigDecimal changePercent,
        String changeDirection
) {
}