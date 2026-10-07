package com.ban.vehicle_management.entrypoint.dto.people.userprofile.request;

import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.entrypoint.dto.shared.address.request.VietnamAddressRequest;
import java.time.LocalDate;

public record CreateUserProfileRequest(
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String phoneNumber,
        String identifyCard,
        String avatarUrl,
        UserProfileStatus status,
        VietnamAddressRequest structuredAddress
) {
    public CreateUserProfileRequest(
            String fullName,
            LocalDate dateOfBirth,
            String gender,
            String phoneNumber,
            String identifyCard,
            String avatarUrl,
            UserProfileStatus status
    ) {
        this(fullName, dateOfBirth, gender, phoneNumber, identifyCard, avatarUrl, status, null);
    }
}

