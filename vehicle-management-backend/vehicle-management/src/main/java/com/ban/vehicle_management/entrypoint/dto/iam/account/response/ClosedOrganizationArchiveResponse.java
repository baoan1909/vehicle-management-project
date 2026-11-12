package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import java.time.Instant;
import java.util.UUID;

public record ClosedOrganizationArchiveResponse(
        UUID organizationId,
        String code,
        String name,
        OrganizationStatus status,
        Instant closedAt,
        UUID closedBy,
        String closureReason,
        UUID closureRequestId
) {}
