package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.util.UUID;

public record OrganizationClosureImpactResponse(
        UUID organizationId,
        String organizationCode,
        String organizationName,
        long affectedParkingLotCount,
        long affectedMemberCount
) {
}
