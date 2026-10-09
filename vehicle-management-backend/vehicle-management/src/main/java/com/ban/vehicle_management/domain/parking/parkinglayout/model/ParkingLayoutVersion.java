package com.ban.vehicle_management.domain.parking.parkinglayout.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLayoutVersion extends AuditableDomainModel {

    private UUID layoutVersionId;
    private UUID zoneId;
    private Long version;
    private LayoutVersionStatus status;
    private java.time.Instant publishedAt;
    private UUID publishedBy;
}