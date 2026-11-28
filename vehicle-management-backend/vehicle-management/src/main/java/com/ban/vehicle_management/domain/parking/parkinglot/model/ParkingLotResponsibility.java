package com.ban.vehicle_management.domain.parking.parkinglot.model;
import java.util.List;
import java.util.UUID;
public record ParkingLotResponsibility(String organizationName, List<Manager> managers) {
    public record Manager(UUID accountId, String displayName) {}
}
