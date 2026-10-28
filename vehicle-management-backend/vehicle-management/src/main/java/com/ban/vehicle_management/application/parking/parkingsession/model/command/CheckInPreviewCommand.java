package com.ban.vehicle_management.application.parking.parkingsession.model.command;

import java.util.UUID;

public record CheckInPreviewCommand(String cardUid, UUID laneId, UUID vehicleTypeId, String licensePlate) {
}

