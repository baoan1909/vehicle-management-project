package com.ban.vehicle_management.entrypoint.dto.iam.organization.request;

import com.ban.vehicle_management.entrypoint.dto.shared.address.request.VietnamAddressRequest;
import java.util.UUID;

public record CreateOrganizationRequest(
        String code,
        String name,
        UUID partnerAdminAccountId,
        VietnamAddressRequest structuredAddress
) {
}
