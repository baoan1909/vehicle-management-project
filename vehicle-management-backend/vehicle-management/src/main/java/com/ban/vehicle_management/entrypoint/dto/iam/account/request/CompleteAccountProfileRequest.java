package com.ban.vehicle_management.entrypoint.dto.iam.account.request;

import java.time.LocalDate;

public record CompleteAccountProfileRequest(
        String fullName,
        String phoneNumber,
        LocalDate dateOfBirth,
        String gender,
        String address,
        String identifyCard,
        String avatarUrl
) {
}
