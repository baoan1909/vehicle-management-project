package com.ban.vehicle_management.application.operations.approvalrequest.model.result;

import java.time.Instant;
import java.util.UUID;

public record CustomerOnboardingApprovalResult(
        RequestInfoResult request,
        AccountInfoResult account,
        ProfileInfoResult profile,
        CustomerInfoResult customer
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
            String phoneNumber
    ) {
    }

    public record CustomerInfoResult(
            UUID customerId,
            String customerCode,
            String customerType,
            String customerStatus,
            String customerApprovalStatus,
            UUID approvedBy,
            Instant approvedAt
    ) {
    }
}
