package com.ban.vehicle_management.application.parking.customerreservation.port.in;

import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import java.time.Instant;
import java.util.UUID;

public interface CustomerReservationAdmissionPortIn {
    CustomerReservation resolveForCheckIn(UUID parkingLotId, UUID zoneId,
            UUID vehicleTypeId, String licensePlate, String qrPayload, Instant now);
    void markCheckedIn(CustomerReservation reservation, UUID parkingSessionId, Instant now);
}
