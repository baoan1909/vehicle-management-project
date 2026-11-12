package com.ban.vehicle_management.application.parking.parkingsession.model.command;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public record CheckInCommand(
        String cardUid,
        UUID laneId,
        UUID vehicleTypeId,
        String licensePlate,
        MultipartFile licensePlateImage,
        MultipartFile vehicleImage,
        MultipartFile personImage,
        String note,
        Boolean plateFormatConfirmed,
        Boolean plateIdentityOverride,
        UUID parkingSpaceId,
        UUID spaceHoldId,
        String subscriptionQrPayload,
        String reservationQrPayload
) {
    public CheckInCommand(
            String cardUid,
            UUID laneId,
            UUID vehicleTypeId,
            String licensePlate,
            MultipartFile licensePlateImage,
            MultipartFile vehicleImage,
            MultipartFile personImage,
            String note
    ) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, licensePlateImage, vehicleImage, personImage, note,
                false, false, null, null, null, null);
    }

    public CheckInCommand(
            String cardUid,
            UUID laneId,
            UUID vehicleTypeId,
            String licensePlate,
            MultipartFile licensePlateImage,
            MultipartFile personImage,
            String note
    ) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, licensePlateImage, null, personImage, note,
                false, false, null, null, null, null);
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
        this(cardUid, laneId, vehicleTypeId, licensePlate, licensePlateImage, null, personImage, note,
                plateFormatConfirmed, plateIdentityOverride, parkingSpaceId, null, null, null);
    }

    public CheckInCommand(String cardUid, UUID laneId, UUID vehicleTypeId, String licensePlate,
            MultipartFile licensePlateImage, MultipartFile vehicleImage, MultipartFile personImage, String note,
            Boolean plateFormatConfirmed, Boolean plateIdentityOverride, UUID parkingSpaceId,
            String subscriptionQrPayload) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, licensePlateImage, vehicleImage, personImage,
                note, plateFormatConfirmed, plateIdentityOverride, parkingSpaceId, null,
                subscriptionQrPayload, null);
    }

    public CheckInCommand(String cardUid, UUID laneId, UUID vehicleTypeId, String licensePlate,
            MultipartFile licensePlateImage, MultipartFile personImage, String note,
            Boolean plateFormatConfirmed, Boolean plateIdentityOverride, UUID parkingSpaceId,
            String subscriptionQrPayload) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, licensePlateImage, null, personImage,
                note, plateFormatConfirmed, plateIdentityOverride, parkingSpaceId, null,
                subscriptionQrPayload, null);
    }

    public CheckInCommand(String cardUid, UUID laneId, UUID vehicleTypeId, String licensePlate,
            MultipartFile licensePlateImage, MultipartFile personImage, String note,
            Boolean plateFormatConfirmed, Boolean plateIdentityOverride, UUID parkingSpaceId,
            String subscriptionQrPayload, String reservationQrPayload) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, licensePlateImage, null, personImage,
                note, plateFormatConfirmed, plateIdentityOverride, parkingSpaceId, null,
                subscriptionQrPayload, reservationQrPayload);
    }
}
