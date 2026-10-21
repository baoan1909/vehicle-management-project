package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "platform_fee_types", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeTypeEntity extends AuditableEntity {

    @Id
    @Column(name = "platform_fee_type_id", nullable = false)
    private UUID platformFeeTypeId;

    @Column(name = "fee_code", nullable = false, unique = true)
    private String feeCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
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

    @Column(name = "is_stackable", nullable = false)
    private boolean stackable;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlatformFeeTypeStatus status;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by")
    private UUID archivedBy;
}
