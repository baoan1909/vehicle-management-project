package com.ban.vehicle_management.entrypoint.dto.iam.organization.request;

import java.util.Set;
import java.util.UUID;

public record AssignParkingManagerRequest(
        UUID parkingManagerAccountId,
        Set<UUID> parkingLotIds
) {
}
