package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response;

import java.time.LocalDate;
import java.util.UUID;

public record InternalEmployeeApprovalAdminResponse(
        RequestInfoResponse request,
        AccountInfoResponse account,
        ProfileInfoResponse profile,
        EmployeeInfoResponse employee
) {
    public record RequestInfoResponse(
            UUID approvalRequestId,
            String requestType,
            String approvalRequestStatus,
            String note,
            UUID requestedBy,
            UUID approvedBy,
            String approvedAt,
            String createdAt,
            String updatedAt
    ) {
    }

    public record AccountInfoResponse(
            UUID accountId,
            String username,
            String email,
            String roleCode,
            String accountStatus
    ) {
    }

    public record ProfileInfoResponse(
            UUID userProfileId,
            String fullName,
            String phoneNumber,
            LocalDate dateOfBirth,
            String gender,
            String address,
            String identifyCard
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
}
