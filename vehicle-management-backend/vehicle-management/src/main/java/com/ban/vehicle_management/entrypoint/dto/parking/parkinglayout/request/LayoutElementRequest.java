package com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.request;

public record LayoutElementRequest(
        String elementType,
        Object geometry,
        Object style,
        String label,
        Integer displayOrder
) {
}
