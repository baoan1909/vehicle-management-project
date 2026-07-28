package com.ban.vehicle_management.application.operations.approvalrequest.model.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InternalEmployeeApprovalResult(
        RequestInfoResult request,
        AccountInfoResult account,
        ProfileInfoResult profile,
        EmployeeInfoResult employee
) {
    public record RequestInfoResult(
            UUID approvalRequestId,
            String requestType,
            String approvalRequestStatus,
            String note,
            UUID requestedBy,
            UUID approvedBy,
            Instant approvedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record AccountInfoResult(
            UUID accountId,
            String username,
            String email,
            String roleCode,
            String accountStatus
    ) {
    }

    public record ProfileInfoResult(
            UUID userProfileId,
            String fullName,
            String phoneNumber,
            LocalDate dateOfBirth,
            String gender,
            String address,
            String identifyCard
    ) {
        public ProfileInfoResult(
                UUID userProfileId,
                String fullName,
                String phoneNumber
        ) {
            this(userProfileId, fullName, phoneNumber, null, null, null, null);
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
}
