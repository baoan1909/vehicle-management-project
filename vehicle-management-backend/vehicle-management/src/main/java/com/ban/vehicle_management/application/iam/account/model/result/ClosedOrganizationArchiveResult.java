package com.ban.vehicle_management.application.iam.account.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import java.time.Instant;
import java.util.UUID;

public record ClosedOrganizationArchiveResult(
        UUID organizationId,
        String code,
        String name,
        OrganizationStatus status,
        Instant closedAt,
        UUID closedBy,
        String closureReason,
        UUID closureRequestId
) {}
