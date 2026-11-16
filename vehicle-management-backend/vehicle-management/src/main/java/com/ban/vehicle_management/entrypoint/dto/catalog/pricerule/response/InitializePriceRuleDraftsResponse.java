package com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.response;

public record InitializePriceRuleDraftsResponse(
        int createdCount,
        boolean alreadyInitialized
) {
}
