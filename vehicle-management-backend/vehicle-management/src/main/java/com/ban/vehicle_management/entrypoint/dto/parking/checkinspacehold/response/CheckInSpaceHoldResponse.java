package com.ban.vehicle_management.entrypoint.dto.parking.checkinspacehold.response;

import com.ban.vehicle_management.shared.enumeration.parking.CheckInSpaceHoldStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CheckInSpaceHoldResponse {
    private UUID holdId;
    private UUID parkingLotId;
    private UUID zoneId;
    private UUID parkingSpaceId;
    private String parkingSpaceCode;
    private String customerType;
    private CheckInSpaceHoldStatus status;
    private Instant expiresAt;
    private Instant serverTime;
}
