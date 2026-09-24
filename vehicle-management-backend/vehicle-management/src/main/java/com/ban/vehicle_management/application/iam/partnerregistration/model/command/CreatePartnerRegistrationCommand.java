package com.ban.vehicle_management.application.iam.partnerregistration.model.command;

public record CreatePartnerRegistrationCommand(
        String organizationCode,
        String organizationName,
        String representativeName,
        String email,
        String phoneNumber,
        String address,
        Integer expectedParkingLotCount,
        String parkingOperationDescription
) {
}
