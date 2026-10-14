package com.ban.vehicle_management.application.parking.parkinglayout.model.result;

import java.util.UUID;

public record PublishPreviewResult(
        UUID zoneId,
        Long draftVersion,
        Long draftRevision,
        int newSpaces,
        int updatedSpaces,
        int archivedSpaces,
        int elementCount,
        int capacityAfterPublish
) {
}
