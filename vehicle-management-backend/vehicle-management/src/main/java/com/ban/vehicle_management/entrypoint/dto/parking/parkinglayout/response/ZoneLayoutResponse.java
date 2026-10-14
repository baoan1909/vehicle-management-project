package com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response;

import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ZoneLayoutResponse {
    private UUID layoutVersionId;
    private UUID zoneId;
    private Long version;
    private Long draftRevision;
    private Instant lastSavedAt;
    private LayoutVersionStatus status;
    private Instant publishedAt;
    private UUID publishedBy;
    private List<LayoutSpaceItemResponse> items;
    private List<LayoutElementResponse> elements;
}
