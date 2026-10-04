package com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response;

import java.time.Instant;
import java.util.UUID;

public record PartnerApplicationStatusResponse(
        UUID accountId,
        UUID approvalRequestId,
        String accountStatus,
        String approvalStatus,
        boolean emailVerified,
        String nextAction,
        String organizationCode,
        String organizationName,
        String reviewNote,
        Instant submittedAt
) {
}
