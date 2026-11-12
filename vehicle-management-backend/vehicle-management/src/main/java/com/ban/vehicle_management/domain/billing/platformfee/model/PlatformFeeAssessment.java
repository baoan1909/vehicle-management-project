package com.ban.vehicle_management.domain.billing.platformfee.model;

import com.ban.vehicle_management.shared.enumeration.billing.FundCustody;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeAssessmentStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeAssessment {
    private UUID platformFeeAssessmentId;
    private UUID invoiceId;
    private UUID paymentId;
    private UUID organizationId;
    private PlatformFeeTransactionType requestedTransactionType;
    private PlatformFeeScopeType resolvedScope;
    private boolean fallbackApplied;
    private BigDecimal basisAmount;
    private BigDecimal totalFeeAmount;
    private String currency;
    private FundCustody fundCustody;
    private FundCustody collector;
    private String amountFingerprint;
    private PlatformFeeAssessmentStatus status;
    private Instant quotedAt;
    private Instant expiresAt;
    private Instant appliedAt;
    private Instant reversedAt;
    private long rowVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private List<PlatformFeeAssessmentLine> lines;
}
