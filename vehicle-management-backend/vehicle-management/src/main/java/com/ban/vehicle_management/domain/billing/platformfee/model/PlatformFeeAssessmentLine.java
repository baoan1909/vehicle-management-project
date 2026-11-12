package com.ban.vehicle_management.domain.billing.platformfee.model;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
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
public class PlatformFeeAssessmentLine {
    private UUID platformFeeAssessmentLineId;
    private UUID platformFeeAssessmentId;
    private UUID feeTypeId;
    private UUID feeRuleVersionId;
    private String feeCode;
    private String feeName;
    private PlatformFeeScopeType scopeType;
    private PlatformFeeTransactionType transactionType;
    private PlatformFeeCalculationType calculationType;
    private PlatformFeeBasis feeBasis;
    private PlatformFeeChargedTo chargedTo;
    private BigDecimal feeRate;
    private BigDecimal basisAmount;
    private BigDecimal calculatedFeeAmount;
    private BigDecimal finalFeeAmount;
    private boolean stackable;
    private int ruleVersion;
    private Instant createdAt;
}
