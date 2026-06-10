package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountProfileStatusResponse(
        boolean onboardingRequired,
        AccountInfoResponse account,
        ProfileInfoResponse profile,
        EmployeeInfoResponse employee,
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
}
