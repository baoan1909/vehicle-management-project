package com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RevenueByDateProjection {
    LocalDate getDate();

    BigDecimal getRevenue();
}
