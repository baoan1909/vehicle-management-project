package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.time.LocalDate;
import java.util.UUID;

public record AccountProfileStatusResponse(
        boolean onboardingRequired,
        AccountInfoResponse account,
        ProfileInfoResponse profile,
        CustomerInfoResponse customer
) {
    public record AccountInfoResponse(
            UUID accountId,
            String accountStatus,
            String username,
            String email,
            String keycloakUserId
    ) {
    }

    public record ProfileInfoResponse(
            UUID userProfileId,
            String fullName,
            LocalDate dateOfBirth,
            String gender,
            String phoneNumber,
            String address,
            String identifyCard,
            String avatarUrl,
            String userProfileStatus
    ) {
    }

    public record CustomerInfoResponse(
            UUID customerId,
            String customerCode,
            String customerType,
            String customerStatus,
            String customerApprovalStatus
    ) {
    }
}
