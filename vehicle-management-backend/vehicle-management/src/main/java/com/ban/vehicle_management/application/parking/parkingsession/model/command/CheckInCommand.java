package com.ban.vehicle_management.application.parking.parkingsession.model.command;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public record CheckInCommand(
        String cardUid,
        UUID laneId,
        String licensePlate,
        MultipartFile licensePlateImage,
        MultipartFile personImage,
        String note
) {
}
