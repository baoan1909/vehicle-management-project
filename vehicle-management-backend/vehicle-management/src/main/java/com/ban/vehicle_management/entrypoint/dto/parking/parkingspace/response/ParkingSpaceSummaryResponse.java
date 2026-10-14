package com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingSpaceSummaryResponse {
    private UUID scopeId;
    private String scopeType;
    private long total;
    private long available;
    private long occupied;
    private long occupiedVisitor;
    private long occupiedRegistered;
    private long occupiedUnknown;
    private long reserved;
    private long maintenance;
    private long subscriptionAllocated;
    private List<ParkingSpaceSummaryResponse> zones;
    private Instant calculatedAt;
}
