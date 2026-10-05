package com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PartnerApplicationStatusResponse(
        UUID accountId,
        UUID approvalRequestId,
        String accountStatus,
        String approvalStatus,
        boolean emailVerified,
        String nextAction,
        boolean hasCompletePersonalProfile,
        boolean hasAvatar,
        boolean hasPersonalAddress,
        boolean hasOrganizationAddress,
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String phoneNumber,
        String identifyCard,
        String avatarUrl,
        PartnerAddressResponse personalAddress,
        String organizationCode,
        String organizationName,
        String representativeName,
        String representativePhoneNumber,
        PartnerAddressResponse organizationAddress,
        String reviewNote,
        Instant submittedAt
) {
}
