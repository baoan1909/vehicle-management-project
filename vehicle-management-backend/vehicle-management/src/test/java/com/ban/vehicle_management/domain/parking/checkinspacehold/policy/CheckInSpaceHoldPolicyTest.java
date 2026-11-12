package com.ban.vehicle_management.domain.parking.checkinspacehold.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.checkinspacehold.model.CheckInSpaceHold;
import com.ban.vehicle_management.shared.enumeration.parking.CheckInSpaceHoldStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckInSpaceHoldPolicyTest {

    private final CheckInSpaceHoldPolicy policy = new CheckInSpaceHoldPolicy();

    @Test
    void shouldInitializeHeldStateWithFutureExpiry() {
        Instant now = Instant.parse("2026-11-10T03:00:00Z");
        CheckInSpaceHold hold = new CheckInSpaceHold();

        policy.initialize(hold, now, now.plusSeconds(120));

        assertEquals(CheckInSpaceHoldStatus.HELD, hold.getStatus());
        assertEquals(now.plusSeconds(120), hold.getExpiresAt());
        assertNull(hold.getConsumedSessionId());
        assertNull(hold.getCancelReason());
    }

    @Test
    void shouldExpireExactlyAtServerDeadline() {
        Instant expiresAt = Instant.parse("2026-11-10T03:02:00Z");
        CheckInSpaceHold hold = heldUntil(expiresAt);

        policy.expireIfDue(hold, expiresAt);

        assertEquals(CheckInSpaceHoldStatus.EXPIRED, hold.getStatus());
    }

    @Test
    void shouldConsumeActiveHoldOnce() {
        Instant now = Instant.parse("2026-11-10T03:00:00Z");
        UUID sessionId = UUID.randomUUID();
        CheckInSpaceHold hold = heldUntil(now.plusSeconds(1));

        policy.consume(hold, sessionId, now);

        assertEquals(CheckInSpaceHoldStatus.CONSUMED, hold.getStatus());
        assertEquals(sessionId, hold.getConsumedSessionId());
    }

    @Test
    void shouldRejectConsumptionAfterExpiry() {
        Instant expiresAt = Instant.parse("2026-11-10T03:02:00Z");
        CheckInSpaceHold hold = heldUntil(expiresAt);

        assertThrows(ConflictException.class,
                () -> policy.consume(hold, UUID.randomUUID(), expiresAt));
        assertEquals(CheckInSpaceHoldStatus.EXPIRED, hold.getStatus());
    }

    @Test
    void shouldCancelOnlyAnActiveHold() {
        CheckInSpaceHold hold = heldUntil(Instant.parse("2026-11-10T03:02:00Z"));

        policy.cancel(hold, "CHECK_IN_CONTEXT_CHANGED");
        policy.cancel(hold, "SECOND_CANCEL");

        assertEquals(CheckInSpaceHoldStatus.CANCELLED, hold.getStatus());
        assertEquals("CHECK_IN_CONTEXT_CHANGED", hold.getCancelReason());
    }

    private CheckInSpaceHold heldUntil(Instant expiresAt) {
        CheckInSpaceHold hold = new CheckInSpaceHold();
        hold.setStatus(CheckInSpaceHoldStatus.HELD);
        hold.setExpiresAt(expiresAt);
        return hold;
    }
}
