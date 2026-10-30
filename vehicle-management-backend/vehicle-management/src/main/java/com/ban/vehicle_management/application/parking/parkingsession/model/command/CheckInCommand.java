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
        Boolean plateIdentityOverride,
        UUID parkingSpaceId,
        String subscriptionQrPayload
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
        this(cardUid, laneId, vehicleTypeId, licensePlate, licensePlateImage, personImage, note,
                false, false, null, null);
    }

    public CheckInCommand(
            String cardUid,
            UUID laneId,
            UUID vehicleTypeId,
            String licensePlate,
            MultipartFile licensePlateImage,
            MultipartFile personImage,
            String note,
            Boolean plateFormatConfirmed,
            Boolean plateIdentityOverride,
            UUID parkingSpaceId
    ) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, licensePlateImage, personImage, note,
                plateFormatConfirmed, plateIdentityOverride, parkingSpaceId, null);
    }
}
