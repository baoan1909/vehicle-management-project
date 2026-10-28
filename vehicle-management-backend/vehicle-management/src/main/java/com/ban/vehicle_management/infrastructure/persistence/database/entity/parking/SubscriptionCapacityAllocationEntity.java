package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_capacity_allocations", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCapacityAllocationEntity extends AuditableEntity {
    @Id
    @Column(name = "allocation_id", nullable = false)
    private UUID allocationId;
    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;
    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;
    @Column(name = "vehicle_type_id", nullable = false)
    private UUID vehicleTypeId;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;
    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_status", nullable = false)
    private SubscriptionCapacityAllocationStatus allocationStatus;
    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

