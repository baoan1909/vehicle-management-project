package com.ban.vehicle_management.domain.billing.platformfee.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import java.math.BigDecimal;
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
public class PlatformFeeRuleVersion extends AuditableDomainModel {

    private UUID platformFeeRuleVersionId;
    private UUID platformFeeTypeId;
    private int version;
    private UUID previousVersionId;
    private BigDecimal feeRate;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private PlatformFeeRuleVersionStatus status;
    private Instant activatedAt;
    private UUID activatedBy;
    private Instant deactivatedAt;
    private UUID deactivatedBy;
    private Instant archivedAt;
    private UUID archivedBy;
}
