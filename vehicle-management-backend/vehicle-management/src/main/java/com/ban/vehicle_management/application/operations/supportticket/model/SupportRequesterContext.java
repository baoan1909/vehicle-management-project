package com.ban.vehicle_management.application.operations.supportticket.model;

import java.util.Set;
import java.util.UUID;

public record SupportRequesterContext(
        UUID accountId,
        String roleCode,
        String displayName,
        String email,
        Set<String> permissionCodes,
        Set<UUID> organizationIds,
        Set<UUID> parkingLotIds
) {
    public SupportRequesterContext {
        permissionCodes = permissionCodes == null ? Set.of() : Set.copyOf(permissionCodes);
        organizationIds = organizationIds == null ? Set.of() : Set.copyOf(organizationIds);
        parkingLotIds = parkingLotIds == null ? Set.of() : Set.copyOf(parkingLotIds);
    }
}
