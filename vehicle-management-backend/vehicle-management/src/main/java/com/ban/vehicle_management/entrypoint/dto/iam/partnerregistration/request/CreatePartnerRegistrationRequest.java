package com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request;

public record CreatePartnerRegistrationRequest(
        String fullName, String username, String password,
        String organizationCode, String organizationName, String representativeName, String email,
        String phoneNumber
) { }
