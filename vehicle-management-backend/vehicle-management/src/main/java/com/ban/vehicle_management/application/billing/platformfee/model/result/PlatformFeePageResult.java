package com.ban.vehicle_management.application.billing.platformfee.model.result;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import java.util.List;

public record PlatformFeePageResult(
        List<PlatformFeeType> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
