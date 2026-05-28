package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request;

public record UpdateParkingLotRequest(
        String code,
        String name,
        String address,
        Integer totalCapacity
) {
}