package com.ban.vehicle_management.application.parking.gate.port.in;

import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import java.util.List;
import java.util.UUID;

public interface GatePortIn {

    Gate createGate(Gate gate);

    Gate getGateById(UUID gateId);

    List<Gate> getGates(UUID zoneId, GateStatus status, String keyword);

    Gate updateGate(UUID gateId, Gate gate);

    void deleteGate(UUID gateId);

    Gate activateGate(UUID gateId);

    Gate markGateMaintenance(UUID gateId);

    Gate closeGate(UUID gateId);
}