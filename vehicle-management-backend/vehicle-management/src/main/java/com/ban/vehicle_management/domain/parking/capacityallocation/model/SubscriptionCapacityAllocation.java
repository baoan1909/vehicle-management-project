package com.ban.vehicle_management.domain.parking.capacityallocation.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCapacityAllocation extends AuditableDomainModel {
    private UUID allocationId;
    private UUID subscriptionId;
    private UUID zoneId;
    private UUID vehicleTypeId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private SubscriptionCapacityAllocationStatus allocationStatus;
    private Instant holdExpiresAt;
    private Long version;
}

