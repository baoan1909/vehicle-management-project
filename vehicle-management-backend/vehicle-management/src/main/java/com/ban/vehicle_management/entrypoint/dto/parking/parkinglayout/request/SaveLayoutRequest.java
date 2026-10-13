package com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.request;

import java.util.List;

public record SaveLayoutRequest(
        Long expectedDraftRevision,
        List<LayoutSpaceItemRequest> items,
        List<LayoutElementRequest> elements
) {
}
