package com.ban.vehicle_management.application.iam.partnerregistration.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PartnerApplicationStatusResult(
        UUID accountId,
        UUID approvalRequestId,
        AccountStatus accountStatus,
        ApprovalRequestStatus approvalStatus,
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
        PartnerAddressResult personalAddress,
        String organizationCode,
        String organizationName,
        String representativeName,
        String representativePhoneNumber,
        PartnerAddressResult organizationAddress,
        String reviewNote,
        Instant submittedAt
) {
}
