package com.ban.vehicle_management.domain.iam.account.model;

import java.util.UUID;

public record OrganizationClosureImpact(
        UUID organizationId,
        String organizationCode,
        String organizationName,
        long affectedParkingLotCount,
        long affectedMemberCount
) {
}
