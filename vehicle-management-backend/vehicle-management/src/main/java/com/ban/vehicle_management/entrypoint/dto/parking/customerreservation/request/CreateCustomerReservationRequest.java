package com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.request;

import java.util.UUID;

public record CreateCustomerReservationRequest(UUID parkingLotId, UUID zoneId,
        UUID parkingSpaceId, UUID vehicleTypeId, String licensePlate,
        boolean policyAccepted) { }
