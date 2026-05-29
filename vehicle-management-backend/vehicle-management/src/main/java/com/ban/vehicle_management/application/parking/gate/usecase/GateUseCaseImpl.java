package com.ban.vehicle_management.application.parking.gate.usecase;

import com.ban.vehicle_management.application.parking.gate.port.in.GatePortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.gate.policy.GatePolicy;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GateUseCaseImpl implements GatePortIn {

    private final GatePortOut gatePortOut;
    private final GatePolicy gatePolicy = new GatePolicy();

    public GateUseCaseImpl(GatePortOut gatePortOut) {
        this.gatePortOut = gatePortOut;
    }

    @Override
    @Transactional
    public Gate createGate(Gate gate) {
        gatePolicy.initialize(gate);
        validateActiveZone(gate.getZoneId());

        if (gatePortOut.existsByZoneIdAndCode(gate.getZoneId(), gate.getCode())) {
            throw new ConflictException("Gate code already exists in this zone");
        }

        gate.setGateId(UUID.randomUUID());
        return gatePortOut.save(gate);
    }

    @Override
    @Transactional(readOnly = true)
    public Gate getGateById(UUID gateId) {
        return gatePortOut.findById(gateId)
                .orElseThrow(() -> new NotFoundException("Gate not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Gate> getGates(UUID zoneId, GateStatus status, String keyword) {
        return gatePortOut.findAll(zoneId, status, normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public Gate updateGate(UUID gateId, Gate gate) {
        Gate existingGate = getGateById(gateId);

        existingGate.setCode(gate.getCode());
        existingGate.setName(gate.getName());

        gatePolicy.initialize(existingGate);

        if (gatePortOut.existsByZoneIdAndCodeAndGateIdNot(
                existingGate.getZoneId(),
                existingGate.getCode(),
                gateId
        )) {
            throw new ConflictException("Gate code already exists in this zone");
        }

        return gatePortOut.save(existingGate);
    }

    @Override
    @Transactional
    public void deleteGate(UUID gateId) {
        Gate existingGate = getGateById(gateId);

        if (existingGate.getStatus() == GateStatus.CLOSED) {
            return;
        }

        ensureNoActiveLanes(gateId);

        gatePolicy.close(existingGate);
        gatePortOut.save(existingGate);
    }

    @Override
    @Transactional
    public Gate activateGate(UUID gateId) {
        Gate existingGate = getGateById(gateId);

        validateActiveZone(existingGate.getZoneId());

        gatePolicy.activate(existingGate);
        return gatePortOut.save(existingGate);
    }

    @Override
    @Transactional
    public Gate markGateMaintenance(UUID gateId) {
        Gate existingGate = getGateById(gateId);

        gatePolicy.markMaintenance(existingGate);
        return gatePortOut.save(existingGate);
    }

    @Override
    @Transactional
    public Gate closeGate(UUID gateId) {
        Gate existingGate = getGateById(gateId);

        if (existingGate.getStatus() == GateStatus.CLOSED) {
            return existingGate;
        }

        ensureNoActiveLanes(gateId);

        gatePolicy.close(existingGate);
        return gatePortOut.save(existingGate);
    }

    private void validateActiveZone(UUID zoneId) {
        if (!gatePortOut.existsActiveZoneById(zoneId)) {
            throw new NotFoundException("Active zone not found");
        }
    }

    private void ensureNoActiveLanes(UUID gateId) {
        if (gatePortOut.hasActiveLanes(gateId)) {
            throw new ConflictException("Gate has active lanes");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}