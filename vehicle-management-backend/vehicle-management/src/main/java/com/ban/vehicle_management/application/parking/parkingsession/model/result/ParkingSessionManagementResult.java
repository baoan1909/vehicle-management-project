package com.ban.vehicle_management.application.parking.parkingsession.model.result;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ParkingSessionManagementResult(
        UUID parkingSessionId,
        UUID cardId,
        String cardNumber,
        String cardUid,
        String cardTypeCode,
        String cardTypeName,
        UUID customerId,
        UUID customerVehicleId,
        UUID vehicleTypeId,
        String vehicleTypeCode,
        String vehicleTypeName,
        UUID zoneId,
        String zoneCode,
        String zoneName,
        UUID parkingLotId,
        String parkingLotCode,
        String parkingLotName,
        String licensePlateIn,
        String licensePlateOut,
        Instant checkInTime,
        Instant checkOutTime,
        ParkingSessionStatus status,
        BigDecimal totalPrice,
        List<EventResult> events
) {
    public record EventResult(
            UUID parkingEventId,
            UUID parkingSessionId,
            UUID laneId,
            String laneCode,
            String laneName,
            ParkingEventType eventType,
            Instant eventTime,
            String licensePlateDetected,
            String licensePlateImagePath,
            String personImagePath,
            UUID actorAccountId,
            String note
    ) {
    }
}
