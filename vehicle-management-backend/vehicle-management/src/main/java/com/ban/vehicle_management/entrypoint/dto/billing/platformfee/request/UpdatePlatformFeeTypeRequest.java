package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;

public record UpdatePlatformFeeTypeRequest(
        String name,
        String description,
        PlatformFeeBasis feeBasis,
        Boolean stackable
) {
}
