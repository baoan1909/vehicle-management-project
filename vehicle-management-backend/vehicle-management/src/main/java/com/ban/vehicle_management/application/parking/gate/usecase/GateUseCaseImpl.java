package com.ban.vehicle_management.application.parking.gate.usecase;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.parking.gate.port.in.GatePortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.gate.policy.GatePolicy;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GateUseCaseImpl implements GatePortIn {

    private final GatePortOut gatePortOut;
    private final ZonePortOut zonePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final GatePolicy gatePolicy = new GatePolicy();

    @Autowired
    public GateUseCaseImpl(
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            OrganizationAccessGuard organizationAccessGuard
    ) {
        this.gatePortOut = gatePortOut;
        this.zonePortOut = zonePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
    }

    // Kept for focused legacy unit tests that exercise only the domain policy.
    public GateUseCaseImpl(GatePortOut gatePortOut) {
        this(gatePortOut, null, null, null);
    }

    @Override
    @Transactional
    public Gate createGate(Gate gate) {
        ensureCanConfigureZone(gate.getZoneId());
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
        return findExistingGate(gateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Gate> getGates(UUID zoneId, GateStatus status, String keyword) {
        ensureCanListZoneResources(zoneId);
        return gatePortOut.findAll(zoneId, status, normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public Gate updateGate(UUID gateId, Gate gate) {
        Gate existingGate = getGateById(gateId);
        ensureCanConfigureZone(existingGate.getZoneId());

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
        ensureCanConfigureZone(existingGate.getZoneId());

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
        ensureCanConfigureZone(existingGate.getZoneId());

        validateActiveZone(existingGate.getZoneId());

        gatePolicy.activate(existingGate);
        return gatePortOut.save(existingGate);
    }

    @Override
    @Transactional
    public Gate markGateMaintenance(UUID gateId) {
        Gate existingGate = getGateById(gateId);
        ensureCanConfigureZone(existingGate.getZoneId());

        gatePolicy.markMaintenance(existingGate);
        return gatePortOut.save(existingGate);
    }

    @Override
    @Transactional
    public Gate closeGate(UUID gateId) {
        Gate existingGate = getGateById(gateId);
        ensureCanConfigureZone(existingGate.getZoneId());

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

    private Gate findExistingGate(UUID gateId) {
        Gate gate = gatePortOut.findById(gateId)
                .orElseThrow(() -> new NotFoundException("Gate not found"));
        ensureCanAccessZone(gate.getZoneId());
        return gate;
    }

    private void ensureCanListZoneResources(UUID zoneId) {
        if (organizationAccessGuard == null) {
            return;
        }
        if (zoneId == null && organizationAccessGuard.isCurrentParkingManager()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Parking manager must select a zone in an assigned parking lot"
            );
        }
        if (zoneId != null) {
            ensureCanAccessZone(zoneId);
        }
    }

    private void ensureCanAccessZone(UUID zoneId) {
        if (organizationAccessGuard == null || zonePortOut == null || parkingLotPortOut == null) {
            return;
        }
        if (zoneId == null) {
            throw new NotFoundException("Zone not found");
        }
        UUID parkingLotId = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"))
                .getParkingLotId();
        organizationAccessGuard.ensureCanAccessParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
    }

    private void ensureCanConfigureZone(UUID zoneId) {
        if (organizationAccessGuard == null || zonePortOut == null || parkingLotPortOut == null) {
            return;
        }
        if (zoneId == null) {
            throw new NotFoundException("Zone not found");
        }
        UUID parkingLotId = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"))
                .getParkingLotId();
        organizationAccessGuard.ensureCanConfigureParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
    }
}
