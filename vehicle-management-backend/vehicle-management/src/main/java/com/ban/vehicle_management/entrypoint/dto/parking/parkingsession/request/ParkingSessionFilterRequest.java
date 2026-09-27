package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ParkingSessionFilterRequest(
        ParkingSessionStatus status,
        UUID vehicleTypeId,
        UUID zoneId,
        UUID parkingLotId,
        LocalDate fromDate,
        LocalDate toDate,
        String keyword
) {
}
