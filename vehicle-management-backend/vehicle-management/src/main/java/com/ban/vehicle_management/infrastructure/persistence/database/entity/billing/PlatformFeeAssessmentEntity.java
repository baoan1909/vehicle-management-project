package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import com.ban.vehicle_management.shared.enumeration.billing.FundCustody;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeAssessmentStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "platform_fee_assessments", schema = "billing")
@Getter
@Setter
public class PlatformFeeAssessmentEntity {
    @Id
    @Column(name = "platform_fee_assessment_id", nullable = false)
    private UUID platformFeeAssessmentId;
    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;
    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;
    @Column(name = "organization_id")
    private UUID organizationId;
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_transaction_type", nullable = false)
    private PlatformFeeTransactionType requestedTransactionType;
    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_scope", nullable = false)
    private PlatformFeeScopeType resolvedScope;
    @Column(name = "fallback_applied", nullable = false)
    private boolean fallbackApplied;
    @Column(name = "basis_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal basisAmount;
    @Column(name = "total_fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFeeAmount;
    @Column(name = "currency", nullable = false)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "fund_custody", nullable = false)
    private FundCustody fundCustody;
    @Enumerated(EnumType.STRING)
    @Column(name = "collector", nullable = false)
    private FundCustody collector;
    @Column(name = "amount_fingerprint", nullable = false)
    private String amountFingerprint;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlatformFeeAssessmentStatus status;
    @Column(name = "quoted_at")
    private Instant quotedAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "applied_at")
    private Instant appliedAt;
    @Column(name = "reversed_at")
    private Instant reversedAt;
    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
}
