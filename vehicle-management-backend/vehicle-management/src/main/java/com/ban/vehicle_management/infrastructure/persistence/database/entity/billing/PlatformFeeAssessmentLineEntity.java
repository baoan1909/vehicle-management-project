package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "platform_fee_assessment_lines", schema = "billing")
@Getter
@Setter
public class PlatformFeeAssessmentLineEntity {
    @Id
    @Column(name = "platform_fee_assessment_line_id", nullable = false)
    private UUID platformFeeAssessmentLineId;
    @Column(name = "platform_fee_assessment_id", nullable = false)
    private UUID platformFeeAssessmentId;
    @Column(name = "fee_type_id", nullable = false)
    private UUID feeTypeId;
    @Column(name = "fee_rule_version_id", nullable = false)
    private UUID feeRuleVersionId;
    @Column(name = "fee_code", nullable = false)
    private String feeCode;
    @Column(name = "fee_name", nullable = false)
    private String feeName;
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private PlatformFeeScopeType scopeType;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private PlatformFeeTransactionType transactionType;
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false)
    private PlatformFeeCalculationType calculationType;
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_basis", nullable = false)
    private PlatformFeeBasis feeBasis;
    @Enumerated(EnumType.STRING)
    @Column(name = "charged_to", nullable = false)
    private PlatformFeeChargedTo chargedTo;
    @Column(name = "fee_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal feeRate;
    @Column(name = "basis_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal basisAmount;
    @Column(name = "calculated_fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal calculatedFeeAmount;
    @Column(name = "final_fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalFeeAmount;
    @Column(name = "is_stackable", nullable = false)
    private boolean stackable;
    @Column(name = "rule_version", nullable = false)
    private int ruleVersion;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
