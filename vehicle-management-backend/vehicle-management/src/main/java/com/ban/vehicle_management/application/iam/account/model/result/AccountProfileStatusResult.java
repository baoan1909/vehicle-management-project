package com.ban.vehicle_management.application.iam.account.model.result;

import java.time.LocalDate;
import java.util.List;
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
            String keycloakUserId,
            String roleCode,
            List<String> permissionCodes
    ) {
        public AccountInfoResult(
                UUID accountId,
                String accountStatus,
                String username,
                String email,
                String keycloakUserId,
                String roleCode
        ) {
            this(accountId, accountStatus, username, email, keycloakUserId, roleCode, List.of());
        }
    }

    public record ProfileInfoResult(
            UUID userProfileId,
            String fullName,
            LocalDate dateOfBirth,
            String gender,
            String phoneNumber,
            String addressDetail,
            String provinceCode,
            String wardCode,
            String districtCode,
            String addressDisplay,
            String identifyCard,
            String avatarUrl,
            String userProfileStatus
    ) {
        public ProfileInfoResult(
                UUID userProfileId,
                String fullName,
                LocalDate dateOfBirth,
                String gender,
                String phoneNumber,
                String addressDisplay,
                String identifyCard,
                String avatarUrl,
                String userProfileStatus
        ) {
            this(
                    userProfileId,
                    fullName,
                    dateOfBirth,
                    gender,
                    phoneNumber,
                    null,
                    null,
                    null,
                    null,
                    addressDisplay,
                    identifyCard,
                    avatarUrl,
                    userProfileStatus
            );
        }
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
