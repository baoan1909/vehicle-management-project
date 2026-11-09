package com.ban.vehicle_management.application.accesscontrol.subscription.model.result;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionSpaceOption(
        UUID parkingSpaceId,
        String code,
        ParkingSpaceStatus status,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height,
        BigDecimal rotation,
        boolean selected,
        boolean selectable
) {
}
