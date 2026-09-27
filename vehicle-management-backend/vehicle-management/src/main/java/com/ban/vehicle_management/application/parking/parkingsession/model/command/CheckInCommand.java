package com.ban.vehicle_management.application.parking.parkingsession.model.command;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public record CheckInCommand(
        String cardUid,
        UUID laneId,
        UUID vehicleTypeId,
        String licensePlate,
        MultipartFile licensePlateImage,
        MultipartFile personImage,
        String note,
        Boolean plateFormatConfirmed,
        Boolean plateIdentityOverride
) {
    public CheckInCommand(
            String cardUid,
            UUID laneId,
            UUID vehicleTypeId,
            String licensePlate,
            MultipartFile licensePlateImage,
            MultipartFile personImage,
            String note
    ) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, licensePlateImage, personImage, note, false, false);
    }
}
