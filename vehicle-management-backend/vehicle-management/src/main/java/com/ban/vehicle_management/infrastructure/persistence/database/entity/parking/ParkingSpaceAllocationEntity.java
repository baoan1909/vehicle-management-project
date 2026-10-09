package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_space_allocations", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpaceAllocationEntity extends AuditableEntity {

    @Id
    @Column(name = "allocation_id", nullable = false)
    private UUID allocationId;

    @Column(name = "parking_space_id", nullable = false)
    private UUID parkingSpaceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_space_id", referencedColumnName = "parking_space_id", insertable = false, updatable = false)
    private ParkingSpaceEntity parkingSpace;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", referencedColumnName = "subscription_id", insertable = false, updatable = false)
    private SubscriptionEntity subscription;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_status", nullable = false)
    private ParkingSpaceAllocationStatus allocationStatus;
}