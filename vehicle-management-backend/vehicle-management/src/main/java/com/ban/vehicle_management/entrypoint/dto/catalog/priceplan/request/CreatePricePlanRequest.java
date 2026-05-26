package com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.request;

import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import java.time.LocalDate;

public record CreatePricePlanRequest(
        String code,
        String name,
        String description,
        PricePlanAppliesTo appliesTo,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}