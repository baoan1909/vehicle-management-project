package com.ban.vehicle_management.application.parking.realtime.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ParkingRealtimeEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID parkingLotId,
        UUID levelId,
        UUID zoneId,
        UUID parkingSpaceId,
        String status,
        Long layoutVersion,
        String correlationId,
        Map<String, String> summary
) {
    public ParkingRealtimeEvent(
            String eventType,
            UUID parkingLotId,
            UUID levelId,
            UUID zoneId,
            UUID parkingSpaceId,
            String status,
            Long layoutVersion,
            Map<String, String> summary
    ) {
        this(UUID.randomUUID(), eventType, Instant.now(), parkingLotId, levelId, zoneId,
                parkingSpaceId, status, layoutVersion, null, summary);
    }
}
