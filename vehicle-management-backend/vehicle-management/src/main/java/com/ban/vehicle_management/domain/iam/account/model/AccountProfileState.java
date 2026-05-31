package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;

import java.time.LocalDate;
import java.util.UUID;

public record AccountProfileState(
        UUID accountId,
        String username,
        String email,
        String keycloakUserId,
        UUID userProfileId,
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String phoneNumber,
        String address,
        String identifyCard,
        String avatarUrl,
        UserProfileStatus userProfileStatus,
        UUID customerId,
        String customerCode,
        CustomerType customerType,
        CustomerStatus customerStatus,
        CustomerApprovalStatus customerApprovalStatus,
        AccountStatus accountStatus
) {
}
