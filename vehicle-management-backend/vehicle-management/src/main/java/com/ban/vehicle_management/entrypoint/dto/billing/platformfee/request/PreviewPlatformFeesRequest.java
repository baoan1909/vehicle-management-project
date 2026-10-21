package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import java.time.Instant;

public record PreviewPlatformFeesRequest(
        PlatformFeeTransactionType transactionType,
        Instant effectiveAt,
        Long grossAmount,
        Long customerPaidAmount
) {
}
