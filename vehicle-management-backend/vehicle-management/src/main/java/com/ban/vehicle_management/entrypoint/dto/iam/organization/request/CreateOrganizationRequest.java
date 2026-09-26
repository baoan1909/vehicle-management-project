package com.ban.vehicle_management.entrypoint.dto.iam.organization.request;

import java.util.UUID;

public record CreateOrganizationRequest(
        String code,
        String name,
        String address,
        UUID partnerAdminAccountId
) {
}
