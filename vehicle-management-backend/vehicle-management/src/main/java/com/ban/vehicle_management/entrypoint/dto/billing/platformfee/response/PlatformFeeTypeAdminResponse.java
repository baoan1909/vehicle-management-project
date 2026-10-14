package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import java.time.Instant;
import java.util.UUID;

public record PlatformFeeTypeAdminResponse(
        UUID platformFeeTypeId,
        String feeCode,
        String name,
        String description,
        PlatformFeeTransactionType transactionType,
        PlatformFeeCalculationType calculationType,
        PlatformFeeBasis feeBasis,
        PlatformFeeChargedTo chargedTo,
        boolean stackable,
        PlatformFeeTypeStatus status,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        Instant archivedAt,
        UUID archivedBy
) {
}
