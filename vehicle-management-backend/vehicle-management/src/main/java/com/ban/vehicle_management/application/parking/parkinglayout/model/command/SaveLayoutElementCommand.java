package com.ban.vehicle_management.application.parking.parkinglayout.model.command;

import com.fasterxml.jackson.databind.JsonNode;

public record SaveLayoutElementCommand(
        String elementType,
        JsonNode geometry,
        JsonNode style,
        String label,
        Integer displayOrder
) {
}
