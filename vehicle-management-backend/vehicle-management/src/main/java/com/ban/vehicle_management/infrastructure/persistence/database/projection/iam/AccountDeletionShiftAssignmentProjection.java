package com.ban.vehicle_management.infrastructure.persistence.database.projection.iam;

import java.util.UUID;

public interface AccountDeletionShiftAssignmentProjection {
    UUID getAssignmentId();
    UUID getParkingLotId();
    String getShiftStatus();
}
