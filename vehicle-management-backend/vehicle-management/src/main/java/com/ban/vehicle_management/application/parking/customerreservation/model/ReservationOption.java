package com.ban.vehicle_management.application.parking.customerreservation.model;

import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReservationOption(UUID zoneId, String zoneName, TrackingMode trackingMode,
        long available, List<SpaceOption> spaces, BigDecimal prepaidAmount) {
    public record SpaceOption(UUID parkingSpaceId, String code) { }
}
