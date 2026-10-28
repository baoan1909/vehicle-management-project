package com.ban.vehicle_management.application.parking.parkingsession.model.result;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import java.util.List;
import java.util.UUID;

public record CheckInPreviewResult(
        UUID parkingLotId,
        UUID zoneId,
        TrackingMode trackingMode,
        String customerType,
        long physicalAvailable,
        long visitorAvailable,
        boolean selectionRequired,
        UUID recommendedParkingSpaceId,
        List<EligibleSpace> eligibleSpaces
) {
    public record EligibleSpace(UUID parkingSpaceId, String code, ParkingSpaceStatus status) {
    }
}

