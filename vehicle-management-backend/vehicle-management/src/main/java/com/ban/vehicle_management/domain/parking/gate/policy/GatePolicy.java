package com.ban.vehicle_management.domain.parking.gate.policy;

import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class GatePolicy {

    public void initialize(Gate gate) {
        requireGate(gate);
        requireField(gate.getZoneId(), "zoneId");
        gate.setCode(TextValidationUtils.normalizeCode(gate.getCode(), "code", 50));
        gate.setName(TextValidationUtils.normalizeRequiredText(gate.getName(), "name", 150));

        if (gate.getStatus() == null) {
            gate.setStatus(GateStatus.ACTIVE);
        }

        validateState(gate);
    }

    public void activate(Gate gate) {
        requireGate(gate);
        gate.setStatus(GateStatus.ACTIVE);
        validateState(gate);
    }

    public void markMaintenance(Gate gate) {
        requireGate(gate);
        gate.setStatus(GateStatus.MAINTENANCE);
        validateState(gate);
    }

    public void close(Gate gate) {
        requireGate(gate);
        gate.setStatus(GateStatus.CLOSED);
        validateState(gate);
    }

    public void validateState(Gate gate) {
        requireGate(gate);
        requireField(gate.getZoneId(), "zoneId");
        requireField(gate.getStatus(), "status");
        gate.setCode(TextValidationUtils.normalizeCode(gate.getCode(), "code", 50));
        gate.setName(TextValidationUtils.normalizeRequiredText(gate.getName(), "name", 150));
    }

    private void requireGate(Gate gate) {
        requireField(gate, "gate");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}