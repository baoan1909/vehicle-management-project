package com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard;

import java.util.UUID;

public interface VehicleTypeCountProjection {
    UUID getVehicleTypeId();

    String getVehicleTypeName();

    long getTotal();
}
