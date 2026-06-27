package com.ban.vehicle_management.domain.operations.shift.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftPolicyTest {

    private final ShiftPolicy policy = new ShiftPolicy();

    @Test
    void shouldInitializeDraftAndClearLifecycleData() {
        Shift shift = baseShift();
        shift.setShiftCode("  hcmute-20260706-morning  ");
        shift.setApprovedAt(Instant.now());
        shift.setApprovedBy(UUID.randomUUID());
        shift.setOpeningCash(BigDecimal.TEN);
        shift.setOpenedAt(Instant.now());
        shift.setOpenedBy(UUID.randomUUID());

        policy.initializeDraft(shift);

        assertEquals(ShiftStatus.DRAFT, shift.getStatus());
        assertEquals("HCMUTE-20260706-MORNING", shift.getShiftCode());
        assertNull(shift.getApprovedAt());
        assertNull(shift.getApprovedBy());
        assertNull(shift.getOpeningCash());
        assertNull(shift.getOpenedAt());
    }

    @Test
    void shouldApproveDraftShift() {
        Shift shift = draftShift();
        UUID accountId = UUID.randomUUID();
        Instant approvedAt = Instant.now();

        policy.approve(shift, accountId, approvedAt);

        assertEquals(ShiftStatus.SCHEDULED, shift.getStatus());
        assertEquals(accountId, shift.getApprovedBy());
        assertEquals(approvedAt, shift.getApprovedAt());
    }

    @Test
    void shouldRejectApprovingNonDraftShift() {
        Shift shift = scheduledShift();

        assertThrows(
                ConflictException.class,
                () -> policy.approve(
                        shift,
                        UUID.randomUUID(),
                        Instant.now()
                )
        );
    }

    @Test
    void shouldOpenScheduledShiftInsideOperationalWindow() {
        Instant now = Instant.now();
        Shift shift = scheduledShift();
        shift.setStartTime(now.minusSeconds(60));
        shift.setEndTime(now.plusSeconds(3600));
        UUID accountId = UUID.randomUUID();

        policy.open(
                shift,
                new BigDecimal("500000"),
                accountId,
                now,
                "  Received cash  "
        );

        assertEquals(ShiftStatus.OPEN, shift.getStatus());
        assertEquals(new BigDecimal("500000"), shift.getOpeningCash());
        assertEquals(accountId, shift.getOpenedBy());
        assertEquals("Received cash", shift.getNote());
    }

    @Test
    void shouldRejectOpeningBeforeStartTime() {
        Instant now = Instant.now();
        Shift shift = scheduledShift();
        shift.setStartTime(now.plusSeconds(60));
        shift.setEndTime(now.plusSeconds(3600));

        assertThrows(
                ConflictException.class,
                () -> policy.open(
                        shift,
                        BigDecimal.ZERO,
                        UUID.randomUUID(),
                        now,
                        null
                )
        );
    }

    @Test
    void shouldRejectNegativeOpeningCash() {
        Instant now = Instant.now();
        Shift shift = scheduledShift();
        shift.setStartTime(now.minusSeconds(60));
        shift.setEndTime(now.plusSeconds(3600));

        assertThrows(
                BadRequestException.class,
                () -> policy.open(
                        shift,
                        new BigDecimal("-1"),
                        UUID.randomUUID(),
                        now,
                        null
                )
        );
    }

    @Test
    void shouldCloseOpenShift() {
        Shift shift = openShift();
        UUID accountId = UUID.randomUUID();
        Instant closedAt = Instant.now();

        policy.close(
                shift,
                new BigDecimal("3200000"),
                accountId,
                closedAt,
                "Counted"
        );

        assertEquals(ShiftStatus.CLOSED, shift.getStatus());
        assertEquals(new BigDecimal("3200000"), shift.getClosingCash());
        assertEquals(accountId, shift.getClosedBy());
        assertEquals(closedAt, shift.getClosedAt());
    }

    @Test
    void shouldCancelDraftShiftAndNormalizeReason() {
        Shift shift = draftShift();
        UUID accountId = UUID.randomUUID();
        Instant cancelledAt = Instant.now();

        policy.cancel(
                shift,
                "  Parking lot maintenance  ",
                accountId,
                cancelledAt
        );

        assertEquals(ShiftStatus.CANCELLED, shift.getStatus());
        assertEquals("Parking lot maintenance", shift.getCancellationReason());
        assertEquals(accountId, shift.getCancelledBy());
    }

    @Test
    void shouldRejectCancellingOpenShift() {
        Shift shift = openShift();

        assertThrows(
                ConflictException.class,
                () -> policy.cancel(
                        shift,
                        "Unexpected closure",
                        UUID.randomUUID(),
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectBlankCancellationReason() {
        Shift shift = draftShift();

        assertThrows(
                BadRequestException.class,
                () -> policy.cancel(
                        shift,
                        "   ",
                        UUID.randomUUID(),
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectInvalidTimeRange() {
        Shift shift = baseShift();
        shift.setEndTime(shift.getStartTime());

        assertThrows(
                BadRequestException.class,
                () -> policy.initializeDraft(shift)
        );
    }

    @Test
    void shouldAcceptValidClosedLifecycleState() {
        assertDoesNotThrow(() -> policy.validateState(closedShift()));
    }

    private Shift draftShift() {
        Shift shift = baseShift();
        policy.initializeDraft(shift);
        return shift;
    }

    private Shift scheduledShift() {
        Shift shift = draftShift();
        policy.approve(shift, UUID.randomUUID(), Instant.now().minusSeconds(60));
        return shift;
    }

    private Shift openShift() {
        Shift shift = scheduledShift();
        Instant now = Instant.now();
        shift.setStartTime(now.minusSeconds(3600));
        shift.setEndTime(now.plusSeconds(3600));
        policy.open(
                shift,
                BigDecimal.ZERO,
                UUID.randomUUID(),
                now,
                null
        );
        return shift;
    }

    private Shift closedShift() {
        Shift shift = openShift();
        policy.close(
                shift,
                BigDecimal.ZERO,
                UUID.randomUUID(),
                Instant.now(),
                null
        );
        return shift;
    }

    private Shift baseShift() {
        Shift shift = new Shift();
        shift.setShiftId(UUID.randomUUID());
        shift.setShiftTemplateId(UUID.randomUUID());
        shift.setParkingLotId(UUID.randomUUID());
        shift.setShiftCode("HCMUTE-20260706-MORNING");
        shift.setShiftDate(LocalDate.of(2026, 7, 6));
        shift.setShiftType(ShiftType.MORNING);
        shift.setStartTime(Instant.parse("2026-07-05T23:00:00Z"));
        shift.setEndTime(Instant.parse("2026-07-06T07:00:00Z"));
        shift.setStatus(ShiftStatus.DRAFT);
        return shift;
    }
}
