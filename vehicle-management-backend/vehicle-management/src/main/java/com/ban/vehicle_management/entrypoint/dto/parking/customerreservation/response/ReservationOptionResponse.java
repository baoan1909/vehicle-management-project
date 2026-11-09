package com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response;

import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReservationOptionResponse(UUID zoneId, String zoneName, TrackingMode trackingMode,
        long available, List<SpaceOptionResponse> spaces, BigDecimal prepaidAmount) {
    public record SpaceOptionResponse(UUID parkingSpaceId, String code) { }
}
