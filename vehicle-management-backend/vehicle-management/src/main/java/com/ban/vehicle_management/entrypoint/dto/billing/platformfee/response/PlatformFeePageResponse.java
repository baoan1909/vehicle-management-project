package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response;

import java.util.List;

public record PlatformFeePageResponse(
        List<PlatformFeeTypeAdminResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
