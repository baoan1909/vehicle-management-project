package com.ban.vehicle_management.domain.parking.checkinspacehold.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.CheckInSpaceHoldStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckInSpaceHold extends AuditableDomainModel {
    private UUID holdId;
    private UUID requestId;
    private UUID parkingSpaceId;
    private UUID parkingLotId;
    private UUID zoneId;
    private UUID laneId;
    private UUID cardId;
    private UUID subscriptionId;
    private UUID customerReservationId;
    private UUID vehicleTypeId;
    private String licensePlateNormalized;
    private String credentialHash;
    private String customerType;
    private UUID operatorAccountId;
    private CheckInSpaceHoldStatus status;
    private Instant expiresAt;
    private UUID consumedSessionId;
    private String cancelReason;
}
