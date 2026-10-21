package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response;

import java.util.List;

public record PlatformFeeTypeDetailResponse(
        PlatformFeeTypeAdminResponse feeType,
        PlatformFeeVersionAdminResponse effectiveVersion,
        PlatformFeeVersionAdminResponse upcomingVersion,
        long totalVersions,
        List<PlatformFeeVersionAdminResponse> recentVersions
) {
}
