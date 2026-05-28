package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;

public record ParkingLotFilterRequest(
        ParkingLotStatus status,
        String keyword
) {
}