package com.ban.vehicle_management.entrypoint.dto.iam.organization.response;

import java.util.Set;
import java.util.UUID;

public record MyParkingLotScopeResponse(Set<UUID> readableParkingLotIds, Set<UUID> operableParkingLotIds) {}
