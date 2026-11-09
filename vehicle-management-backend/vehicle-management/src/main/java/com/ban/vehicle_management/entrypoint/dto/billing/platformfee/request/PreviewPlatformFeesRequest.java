package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import java.time.Instant;
import jakarta.validation.constraints.NotNull;

public record PreviewPlatformFeesRequest(
        PlatformFeeTransactionType transactionType,
        Instant effectiveAt,
        @NotNull Long grossAmount,
        @NotNull Long customerPaidAmount
) {
}
