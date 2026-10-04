package com.ban.vehicle_management.application.iam.partnerregistration.model.command;

public record CreatePartnerRegistrationCommand(
        String fullName,
        String username,
        String password,
        String organizationCode,
        String organizationName,
        String representativeName,
        String email,
        String phoneNumber
) {
}
