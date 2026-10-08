package com.ban.vehicle_management.entrypoint.dto.iam.account.request;

import com.ban.vehicle_management.entrypoint.dto.shared.address.request.VietnamAddressRequest;
import java.time.LocalDate;

public record UpdateAccountProfileRequest(
        String fullName,
        String phoneNumber,
        LocalDate dateOfBirth,
        String gender,
        String identifyCard,
        String avatarUrl,
        VietnamAddressRequest structuredAddress,
        String organizationCode,
        String organizationName,
        VietnamAddressRequest organizationAddress
) {
    public UpdateAccountProfileRequest(
            String fullName,
            String phoneNumber,
            LocalDate dateOfBirth,
            String gender,
            String identifyCard,
            String avatarUrl
    ) {
        this(fullName, phoneNumber, dateOfBirth, gender, identifyCard, avatarUrl, null, null, null, null);
    }
}
