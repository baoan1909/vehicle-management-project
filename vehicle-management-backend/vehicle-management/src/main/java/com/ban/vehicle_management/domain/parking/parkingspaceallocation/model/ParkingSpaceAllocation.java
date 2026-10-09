package com.ban.vehicle_management.domain.parking.parkingspaceallocation.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
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
public class ParkingSpaceAllocation extends AuditableDomainModel {

    private UUID allocationId;
    private UUID parkingSpaceId;
    private UUID subscriptionId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Instant holdExpiresAt;
    private ParkingSpaceAllocationStatus allocationStatus;
}