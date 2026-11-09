package com.ban.vehicle_management.infrastructure.persistence.database.projection.iam;

import java.util.UUID;

public interface AccountDeletionParkingResourceProjection {
    UUID getResourceId();
    UUID getParkingLotId();
}
