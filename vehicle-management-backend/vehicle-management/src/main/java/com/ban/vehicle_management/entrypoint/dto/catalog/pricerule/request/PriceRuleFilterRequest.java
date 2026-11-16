package com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.request;

import com.ban.vehicle_management.shared.enumeration.catalog.CatalogLifecycleStatus;
import java.util.UUID;

public record PriceRuleFilterRequest(
        UUID parkingLotId,
        UUID pricePlanId,
        UUID vehicleTypeId,
        UUID ticketTypeId,
        Boolean isActive,
        String keyword,
        CatalogLifecycleStatus lifecycleStatus
) {
    public PriceRuleFilterRequest(UUID pricePlanId, UUID vehicleTypeId, UUID ticketTypeId, Boolean isActive, String keyword) {
        this(null, pricePlanId, vehicleTypeId, ticketTypeId, isActive, keyword, null);
    }
}
