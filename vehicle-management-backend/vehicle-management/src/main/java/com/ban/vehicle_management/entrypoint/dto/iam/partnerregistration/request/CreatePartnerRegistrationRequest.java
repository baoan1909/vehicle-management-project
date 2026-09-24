package com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request;

public record CreatePartnerRegistrationRequest(
        String organizationCode, String organizationName, String representativeName, String email,
        String phoneNumber, String address, Integer expectedParkingLotCount, String parkingOperationDescription
) { }
