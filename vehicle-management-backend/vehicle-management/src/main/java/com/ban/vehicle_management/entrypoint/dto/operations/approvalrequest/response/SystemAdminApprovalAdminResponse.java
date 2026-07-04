package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response;

import java.util.UUID;

public record SystemAdminApprovalAdminResponse(
        RequestInfoResponse request,
        AccountInfoResponse account,
        ProfileInfoResponse profile
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
            String phoneNumber
    ) {
    }
}
