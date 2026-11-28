package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response;
import java.util.List;
import java.util.UUID;
public record ParkingLotResponsibilityResponse(String organizationName, List<Manager> managers) {
    public record Manager(UUID accountId, String displayName) {}
}
