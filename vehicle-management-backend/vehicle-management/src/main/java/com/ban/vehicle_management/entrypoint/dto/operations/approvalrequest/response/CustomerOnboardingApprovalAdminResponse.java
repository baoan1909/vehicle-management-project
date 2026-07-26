package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response;

import java.time.LocalDate;
import java.util.UUID;

public record CustomerOnboardingApprovalAdminResponse(
        RequestInfoResponse request,
        AccountInfoResponse account,
        ProfileInfoResponse profile,
        CustomerInfoResponse customer
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

    public record CustomerInfoResponse(
            UUID customerId,
            String customerCode,
            String customerType,
            String customerStatus,
            String customerApprovalStatus,
            UUID approvedBy,
            String approvedAt
    ) {
    }
}
