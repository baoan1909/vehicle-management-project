package com.ban.vehicle_management.application.iam.account.model.result;

import java.time.LocalDate;
import java.util.UUID;

public record AccountProfileStatusResult(
        boolean onboardingRequired,
        AccountInfoResult account,
        ProfileInfoResult profile,
        EmployeeInfoResult employee,
        CustomerInfoResult customer
) {
    public record AccountInfoResult(
            UUID accountId,
            String accountStatus,
            String username,
            String email,
            String keycloakUserId
    ) {
    }

    public record ProfileInfoResult(
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

    public record EmployeeInfoResult(
            UUID employeeId,
            String employeeCode,
            String jobTitle,
            LocalDate hiredAt,
            String employeeStatus
    ) {
    }

    public record CustomerInfoResult(
            UUID customerId,
            String customerCode,
            String customerType,
            String customerStatus,
            String customerApprovalStatus
    ) {
    }
}
