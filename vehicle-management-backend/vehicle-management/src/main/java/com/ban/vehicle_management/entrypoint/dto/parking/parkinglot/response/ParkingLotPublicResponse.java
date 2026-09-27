package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response;

import java.util.UUID;

public record ParkingLotPublicResponse(UUID parkingLotId, String name, String address) {
}
