package com.ban.vehicle_management.application.iam.account.model.result;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AccountProfileStatusResult(
        boolean onboardingRequired,
        AccountInfoResult account,
        ProfileInfoResult profile,
        EmployeeInfoResult employee,
        CustomerInfoResult customer,
        PartnerApplicationInfoResult partnerApplication,
        OrganizationInfoResult organization
) {
    public AccountProfileStatusResult(
            boolean onboardingRequired,
            AccountInfoResult account,
            ProfileInfoResult profile,
            EmployeeInfoResult employee,
            CustomerInfoResult customer,
            OrganizationInfoResult organization
    ) {
        this(onboardingRequired, account, profile, employee, customer, null, organization);
    }
    public record AccountInfoResult(
            UUID accountId,
            String accountStatus,
            String username,
            String email,
            String keycloakUserId,
            String roleCode,
            List<String> permissionCodes,
            Boolean emailVerified
    ) {
        public AccountInfoResult(
                UUID accountId,
                String accountStatus,
                String username,
                String email,
                String keycloakUserId,
                String roleCode
        ) {
            this(accountId, accountStatus, username, email, keycloakUserId, roleCode, List.of(), null);
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

    public record PartnerApplicationInfoResult(
            UUID approvalRequestId,
            String approvalStatus,
            String reviewNote
    ) {
    }

    public record OrganizationInfoResult(
            UUID organizationId,
            String organizationCode,
            String organizationName,
            String addressDetail,
            String provinceCode,
            String wardCode,
            String districtCode,
            String addressDisplay,
            String organizationStatus
    ) {
    }
}
