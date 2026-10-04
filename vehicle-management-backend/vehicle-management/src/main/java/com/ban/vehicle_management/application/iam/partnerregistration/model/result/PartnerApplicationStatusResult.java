package com.ban.vehicle_management.application.iam.partnerregistration.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record PartnerApplicationStatusResult(
        UUID accountId,
        UUID approvalRequestId,
        AccountStatus accountStatus,
        ApprovalRequestStatus approvalStatus,
        boolean emailVerified,
        String nextAction,
        String organizationCode,
        String organizationName,
        String reviewNote,
        Instant submittedAt
) {
}
