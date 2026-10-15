package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;

public record PlatformFeeFilterRequest(
        String keyword,
        PlatformFeeTypeStatus status,
        PlatformFeeTransactionType transactionType,
        PlatformFeeBasis feeBasis,
        Integer page,
        Integer size
) {
}
