package com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.util.UUID;

public record LayoutSpaceItemRequest(
        UUID parkingSpaceId,
        @JsonAlias("code") String spaceCode,
        UUID vehicleTypeId,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height,
        BigDecimal rotation
) {
}
