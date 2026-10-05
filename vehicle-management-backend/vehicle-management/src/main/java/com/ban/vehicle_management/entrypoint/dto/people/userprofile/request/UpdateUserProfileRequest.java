package com.ban.vehicle_management.entrypoint.dto.people.userprofile.request;

import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.entrypoint.dto.shared.address.request.VietnamAddressRequest;
import java.time.LocalDate;

public record UpdateUserProfileRequest(
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String phoneNumber,
        String address,
        String identifyCard,
        String avatarUrl,
        UserProfileStatus status,
        VietnamAddressRequest structuredAddress
) {
    public UpdateUserProfileRequest(
            String fullName,
            LocalDate dateOfBirth,
            String gender,
            String phoneNumber,
            String address,
            String identifyCard,
            String avatarUrl,
            UserProfileStatus status
    ) {
        this(fullName, dateOfBirth, gender, phoneNumber, address, identifyCard, avatarUrl, status, null);
    }
}

