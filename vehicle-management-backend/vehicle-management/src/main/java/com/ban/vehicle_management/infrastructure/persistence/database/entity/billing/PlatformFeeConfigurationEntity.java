package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "platform_fee_configuration", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeConfigurationEntity {

    @Id
    @Column(name = "configuration_key", nullable = false, length = 30)
    private String configurationKey;

    @Column(name = "max_partner_total_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal maxPartnerTotalRate;

    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
