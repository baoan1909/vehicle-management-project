package com.ban.vehicle_management.domain.parking.parkinglevel.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkingLevelPolicyTest {

    private final ParkingLevelPolicy policy = new ParkingLevelPolicy();

    @Test
    void shouldInitializeWithDefaults() {
        ParkingLevel level = new ParkingLevel();
        level.setParkingLotId(UUID.randomUUID());
        level.setCode("l1");
        level.setName("Level 1");

        policy.initialize(level);

        assertEquals("L1", level.getCode());
        assertEquals(ParkingLevelStatus.ACTIVE, level.getStatus());
    }

    @Test
    void shouldRejectNonPositiveCanvas() {
        ParkingLevel level = new ParkingLevel();
        level.setParkingLotId(UUID.randomUUID());
        level.setCode("L1");
        level.setName("Level 1");
        level.setCanvasWidth(BigDecimal.ZERO);
        level.setCanvasHeight(BigDecimal.valueOf(100));

        assertThrows(BadRequestException.class, () -> policy.initialize(level));
    }

    @Test
    void shouldRejectNonPositiveFloorHeight() {
        ParkingLevel level = new ParkingLevel();
        level.setParkingLotId(UUID.randomUUID());
        level.setCode("L1");
        level.setName("Level 1");
        level.setCanvasWidth(BigDecimal.valueOf(100));
        level.setCanvasHeight(BigDecimal.valueOf(100));
        level.setFloorHeight(BigDecimal.ZERO);

        assertThrows(BadRequestException.class, () -> policy.initialize(level));
    }
}
