package com.ban.vehicle_management.entrypoint.dto.people.userprofile.request;

import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;
import java.time.LocalDate;

public record CreateUserProfileRequest(
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String phoneNumber,
        String address,
        String identifyCard,
        String avatarUrl,
        UserProfileStatus status
) {
}
