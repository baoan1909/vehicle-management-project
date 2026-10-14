package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "platform_fee_rule_versions", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeRuleVersionEntity extends AuditableEntity {

    @Id
    @Column(name = "platform_fee_rule_version_id", nullable = false)
    private UUID platformFeeRuleVersionId;

    @Column(name = "platform_fee_type_id", nullable = false)
    private UUID platformFeeTypeId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "previous_version_id")
    private UUID previousVersionId;

    @Column(name = "fee_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal feeRate;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlatformFeeRuleVersionStatus status;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "activated_by")
    private UUID activatedBy;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivated_by")
    private UUID deactivatedBy;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by")
    private UUID archivedBy;
}
