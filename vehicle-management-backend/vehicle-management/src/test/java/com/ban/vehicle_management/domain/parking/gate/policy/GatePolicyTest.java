package com.ban.vehicle_management.domain.parking.gate.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GatePolicyTest {

    private final GatePolicy gatePolicy = new GatePolicy();

    @Test
    void shouldNormalizeFieldsAndSetDefaultsWhenInitialize() {
        Gate gate = validGate();
        gate.setCode(" moto-gate-01 ");
        gate.setName(" Cong xe may so 1 ");
        gate.setStatus(null);

        gatePolicy.initialize(gate);

        assertEquals("MOTO-GATE-01", gate.getCode());
        assertEquals("Cong xe may so 1", gate.getName());
        assertEquals(GateStatus.ACTIVE, gate.getStatus());
    }

    @Test
    void shouldKeepExistingStatusWhenInitialize() {
        Gate gate = validGate();
        gate.setStatus(GateStatus.MAINTENANCE);

        gatePolicy.initialize(gate);

        assertEquals(GateStatus.MAINTENANCE, gate.getStatus());
    }

    @Test
    void shouldRejectNullGate() {
        assertThrows(BadRequestException.class, () -> gatePolicy.initialize(null));
    }

    @Test
    void shouldRejectNullZoneId() {
        Gate gate = validGate();
        gate.setZoneId(null);

        assertThrows(BadRequestException.class, () -> gatePolicy.initialize(gate));
    }

    @Test
    void shouldRejectBlankCode() {
        Gate gate = validGate();
        gate.setCode(" ");

        assertThrows(BadRequestException.class, () -> gatePolicy.initialize(gate));
    }

    @Test
    void shouldRejectBlankName() {
        Gate gate = validGate();
        gate.setName(" ");

        assertThrows(BadRequestException.class, () -> gatePolicy.initialize(gate));
    }

    @Test
    void shouldRejectNameExceedingSchemaLength() {
        Gate gate = validGate();
        gate.setName("A".repeat(151));

        assertThrows(BadRequestException.class, () -> gatePolicy.initialize(gate));
    }

    @Test
    void shouldActivateGate() {
        Gate gate = validGate();
        gate.setStatus(GateStatus.CLOSED);

        gatePolicy.activate(gate);

        assertEquals(GateStatus.ACTIVE, gate.getStatus());
    }

    @Test
    void shouldMarkGateMaintenance() {
        Gate gate = validGate();

        gatePolicy.markMaintenance(gate);

        assertEquals(GateStatus.MAINTENANCE, gate.getStatus());
    }

    @Test
    void shouldCloseGate() {
        Gate gate = validGate();

        gatePolicy.close(gate);

        assertEquals(GateStatus.CLOSED, gate.getStatus());
    }

    private Gate validGate() {
        Gate gate = new Gate();
        gate.setZoneId(UUID.randomUUID());
        gate.setCode("MOTO-GATE-01");
        gate.setName("Cong xe may so 1");
        gate.setStatus(GateStatus.ACTIVE);
        return gate;
    }
}
