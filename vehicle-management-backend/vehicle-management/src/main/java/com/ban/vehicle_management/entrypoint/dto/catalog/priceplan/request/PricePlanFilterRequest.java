package com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.request;

import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import java.time.LocalDate;

public record PricePlanFilterRequest(
        Boolean isActive,
        PricePlanAppliesTo appliesTo,
        LocalDate effectiveDate,
        String keyword
) {
}