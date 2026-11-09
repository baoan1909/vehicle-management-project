package com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ParkingPlateLookupResponse(List<Session> sessions, List<Reservation> reservations) {
    public record Session(UUID parkingLotId, BigDecimal latitude, BigDecimal longitude,
            String licensePlate, String vehicleTypeName, Instant checkInTime,
            String parkingLotName, String parkingLevelName, String zoneName,
            String parkingSpaceCode) { }

    public record Reservation(UUID parkingLotId, BigDecimal latitude, BigDecimal longitude,
            String licensePlate, BigDecimal prepaidAmount, Instant paidAt,
            Instant checkInDeadline, String parkingLotName, String parkingLevelName,
            String zoneName, String parkingSpaceCode) { }
}
