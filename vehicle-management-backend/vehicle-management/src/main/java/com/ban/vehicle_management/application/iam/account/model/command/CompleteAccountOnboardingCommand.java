package com.ban.vehicle_management.application.iam.account.model.command;

import java.time.LocalDate;

public record CompleteAccountOnboardingCommand(
        String fullName,
        String phoneNumber,
        LocalDate dateOfBirth,
        String gender,
        String address,
        String identifyCard,
        String avatarUrl
) {
}
