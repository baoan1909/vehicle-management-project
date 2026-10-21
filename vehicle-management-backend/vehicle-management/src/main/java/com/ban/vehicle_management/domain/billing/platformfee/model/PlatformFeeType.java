package com.ban.vehicle_management.domain.billing.platformfee.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeType extends AuditableDomainModel {

    private UUID platformFeeTypeId;
    private String feeCode;
    private String name;
    private String description;
    private PlatformFeeTransactionType transactionType;
    private PlatformFeeCalculationType calculationType;
    private PlatformFeeBasis feeBasis;
    private PlatformFeeChargedTo chargedTo;
    private boolean stackable;
    private PlatformFeeTypeStatus status;
    private Instant archivedAt;
    private UUID archivedBy;
}
