package com.ban.vehicle_management.domain.operations.shift.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.shared.enumeration.ShiftStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftPolicyTest {

    private final ShiftPolicy shiftPolicy = new ShiftPolicy();

    @Test
    void shouldInitializeShiftWithDefaults() {
        Shift shift = new Shift();
        shift.setShiftCode(" SHIFT-001 ");
        shift.setParkingLotId(UUID.randomUUID());
        shift.setStartTime(Instant.parse("2026-05-15T01:00:00Z"));

        shiftPolicy.initialize(shift);

        assertEquals("SHIFT-001", shift.getShiftCode());
        assertEquals(ShiftStatus.OPEN, shift.getStatus());
        assertEquals(BigDecimal.ZERO, shift.getOpeningCash());
    }

    @Test
    void shouldCloseOpenShift() {
        Shift shift = validShift();
        Instant endTime = Instant.parse("2026-05-15T09:00:00Z");
        BigDecimal closingCash = new BigDecimal("900000");

        shiftPolicy.close(shift, endTime, closingCash);

        assertEquals(ShiftStatus.CLOSED, shift.getStatus());
        assertEquals(endTime, shift.getEndTime());
        assertEquals(closingCash, shift.getClosingCash());
    }

    @Test
    void shouldRejectOpenShiftWithEndTime() {
        Shift shift = validShift();
        shift.setEndTime(Instant.parse("2026-05-15T09:00:00Z"));

        assertThrows(BadRequestException.class, () -> shiftPolicy.validateState(shift));
    }

    @Test
    void shouldCancelOpenShift() {
        Shift shift = validShift();
        shiftPolicy.cancel(shift);

        assertEquals(ShiftStatus.CANCELLED, shift.getStatus());
        assertNull(shift.getEndTime());
        assertNull(shift.getClosingCash());
    }

    @Test
    void shouldRejectShiftCodeExceedingSchemaLength() {
        Shift shift = new Shift();
        shift.setShiftCode("A".repeat(51));
        shift.setParkingLotId(UUID.randomUUID());
        shift.setStartTime(Instant.parse("2026-05-15T01:00:00Z"));

        assertThrows(BadRequestException.class, () -> shiftPolicy.initialize(shift));
    }

    private Shift validShift() {
        Shift shift = new Shift();
        shift.setShiftId(UUID.randomUUID());
        shift.setShiftCode("SHIFT-001");
        shift.setParkingLotId(UUID.randomUUID());
        shift.setStartTime(Instant.parse("2026-05-15T01:00:00Z"));
        shift.setStatus(ShiftStatus.OPEN);
        shift.setOpeningCash(new BigDecimal("500000"));
        return shift;
    }
}

