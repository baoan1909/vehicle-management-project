package com.ban.vehicle_management.infrastructure.persistence.database.projection.iam;

import java.util.UUID;

public interface AccountDeletionResourceScopeProjection {
    UUID getResourceId();
    UUID getOrganizationId();
    UUID getParkingLotId();
}
