package com.ban.vehicle_management.application.accesscontrol.subscription.model.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionSpaceSelection(
        UUID subscriptionId,
        UUID selectedParkingSpaceId,
        String selectedParkingSpaceCode,
        BigDecimal canvasWidth,
        BigDecimal canvasHeight,
        Instant selectionExpiresAt,
        List<SubscriptionSpaceOption> spaces
) {
}
