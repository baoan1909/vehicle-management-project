package com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PublishPreviewResponse {
    private UUID zoneId;
    private Long draftVersion;
    private Long draftRevision;
    private int newSpaces;
    private int updatedSpaces;
    private int archivedSpaces;
    private int elementCount;
    private int capacityAfterPublish;
}
