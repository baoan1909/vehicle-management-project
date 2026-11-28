package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;
import java.util.UUID;
public interface ParkingLotResponsibilityProjection {
    String getOrganizationName();
    UUID getAccountId();
    String getDisplayName();
}
