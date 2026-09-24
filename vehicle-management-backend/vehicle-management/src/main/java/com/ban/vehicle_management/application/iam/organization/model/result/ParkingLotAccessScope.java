package com.ban.vehicle_management.application.iam.organization.model.result;

import java.util.Set;
import java.util.UUID;

public record ParkingLotAccessScope(
        boolean unrestricted,
        Set<UUID> organizationIds,
        Set<UUID> parkingLotIds
) {
    public static ParkingLotAccessScope unrestrictedScope() {
        return new ParkingLotAccessScope(true, Set.of(), Set.of());
    }
}
