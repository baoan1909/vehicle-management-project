package com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.request;

import java.time.LocalDate;

public record UpdatePricePlanRequest(
        String code,
        String name,
        String description,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}