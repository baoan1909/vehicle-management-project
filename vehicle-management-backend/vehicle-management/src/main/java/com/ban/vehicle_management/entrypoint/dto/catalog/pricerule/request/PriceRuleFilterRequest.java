package com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.request;

import java.util.UUID;

public record PriceRuleFilterRequest(
        UUID pricePlanId,
        UUID vehicleTypeId,
        UUID ticketTypeId,
        Boolean isActive,
        String keyword
) {
}