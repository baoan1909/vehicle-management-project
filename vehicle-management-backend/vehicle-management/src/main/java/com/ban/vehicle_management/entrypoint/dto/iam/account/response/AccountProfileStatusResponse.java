package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountProfileStatusResponse(
        boolean onboardingRequired,
        AccountInfoResponse account,
        ProfileInfoResponse profile,
        EmployeeInfoResponse employee,
        CustomerInfoResponse customer,
        PartnerApplicationInfoResponse partnerApplication,
        OrganizationInfoResponse organization
) {
    public AccountProfileStatusResponse(
            boolean onboardingRequired,
            AccountInfoResponse account,
            ProfileInfoResponse profile,
            EmployeeInfoResponse employee,
            CustomerInfoResponse customer,
            OrganizationInfoResponse organization
    ) {
        this(onboardingRequired, account, profile, employee, customer, null, organization);
    }
    public record AccountInfoResponse(
            UUID accountId,
            String accountStatus,
            String username,
            String email,
            String keycloakUserId,
            String roleCode,
            List<String> permissionCodes,
            Boolean emailVerified
    ) {
        public AccountInfoResponse(
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

    public record ProfileInfoResponse(
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
        public ProfileInfoResponse(
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

    public record EmployeeInfoResponse(
            UUID employeeId,
            String employeeCode,
            String jobTitle,
            LocalDate hiredAt,
            String employeeStatus
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

    public record PartnerApplicationInfoResponse(
            UUID approvalRequestId,
            String approvalStatus,
            String reviewNote
    ) {
    }

    public record OrganizationInfoResponse(
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
