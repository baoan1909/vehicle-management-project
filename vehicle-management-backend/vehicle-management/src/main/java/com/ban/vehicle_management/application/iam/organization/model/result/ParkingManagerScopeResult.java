package com.ban.vehicle_management.application.iam.organization.model.result;

import java.util.Set;
import java.util.UUID;

public record ParkingManagerScopeResult(Set<UUID> parkingManagerAccountIds) {
}
