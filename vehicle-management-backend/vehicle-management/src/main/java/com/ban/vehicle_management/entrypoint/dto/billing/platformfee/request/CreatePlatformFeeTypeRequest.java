package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;

public record CreatePlatformFeeTypeRequest(
        String feeCode,
        String name,
        String description,
        PlatformFeeTransactionType transactionType,
        PlatformFeeCalculationType calculationType,
        PlatformFeeBasis feeBasis,
        PlatformFeeChargedTo chargedTo,
        Boolean stackable
) {
}
