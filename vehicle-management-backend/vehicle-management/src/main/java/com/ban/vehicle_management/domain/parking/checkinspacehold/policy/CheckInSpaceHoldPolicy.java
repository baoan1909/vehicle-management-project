package com.ban.vehicle_management.domain.parking.checkinspacehold.policy;

import com.ban.vehicle_management.domain.parking.checkinspacehold.model.CheckInSpaceHold;
import com.ban.vehicle_management.shared.enumeration.parking.CheckInSpaceHoldStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
import java.util.UUID;

public class CheckInSpaceHoldPolicy {

    public void initialize(CheckInSpaceHold hold, Instant now, Instant expiresAt) {
        if (hold == null || now == null || expiresAt == null || !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("A check-in space hold requires a future expiry");
        }
        hold.setStatus(CheckInSpaceHoldStatus.HELD);
        hold.setExpiresAt(expiresAt);
        hold.setConsumedSessionId(null);
        hold.setCancelReason(null);
    }

    public void expireIfDue(CheckInSpaceHold hold, Instant now) {
        if (hold.getStatus() == CheckInSpaceHoldStatus.HELD
                && !hold.getExpiresAt().isAfter(now)) {
            hold.setStatus(CheckInSpaceHoldStatus.EXPIRED);
        }
    }

    public void cancel(CheckInSpaceHold hold, String reason) {
        if (hold.getStatus() == CheckInSpaceHoldStatus.HELD) {
            hold.setStatus(CheckInSpaceHoldStatus.CANCELLED);
            hold.setCancelReason(reason);
        }
    }

    public void consume(CheckInSpaceHold hold, UUID parkingSessionId, Instant now) {
        expireIfDue(hold, now);
        if (hold.getStatus() != CheckInSpaceHoldStatus.HELD) {
            throw new ConflictException("Check-in parking-space hold is no longer active");
        }
        if (parkingSessionId == null) {
            throw new IllegalArgumentException("parkingSessionId must not be null");
        }
        hold.setStatus(CheckInSpaceHoldStatus.CONSUMED);
        hold.setConsumedSessionId(parkingSessionId);
    }
}
