package com.ban.vehicle_management.application.parking.parkinglayout.model.result;

import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ZoneLayoutResult(
        ParkingLayoutVersion version,
        List<LayoutItemView> items,
        List<LayoutElementView> elements
) {
    public record LayoutItemView(
            UUID layoutItemId,
            UUID layoutVersionId,
            UUID parkingSpaceId,
            String spaceCode,
            UUID vehicleTypeId,
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height,
            BigDecimal rotation
    ) {
    }

    public record LayoutElementView(
            UUID layoutElementId,
            String elementType,
            JsonNode geometry,
            JsonNode style,
            String label,
            Integer displayOrder
    ) {
    }
}