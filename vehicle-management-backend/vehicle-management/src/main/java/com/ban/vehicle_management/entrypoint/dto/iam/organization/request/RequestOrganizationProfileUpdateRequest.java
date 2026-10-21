package com.ban.vehicle_management.entrypoint.dto.iam.organization.request;

import com.ban.vehicle_management.entrypoint.dto.shared.address.request.VietnamAddressRequest;

/** Partner Admin can only propose a change; the approved record stays unchanged until review. */
public record RequestOrganizationProfileUpdateRequest(
        String name,
        VietnamAddressRequest structuredAddress
) {
}
