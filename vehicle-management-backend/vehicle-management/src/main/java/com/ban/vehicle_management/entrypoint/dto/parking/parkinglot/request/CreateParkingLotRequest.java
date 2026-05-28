package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request;

public record CreateParkingLotRequest(
        String code,
        String name,
        String address,
        Integer totalCapacity
) {
}