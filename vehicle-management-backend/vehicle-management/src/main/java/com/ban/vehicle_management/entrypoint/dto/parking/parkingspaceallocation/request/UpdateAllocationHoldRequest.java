package com.ban.vehicle_management.entrypoint.dto.parking.parkingspaceallocation.request;

import java.time.Instant;

public record UpdateAllocationHoldRequest(Instant holdExpiresAt) {
}
