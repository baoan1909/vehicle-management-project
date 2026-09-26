package com.ban.vehicle_management.application.parking.zone.usecase;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.zone.port.in.ZonePortIn;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.parking.zone.policy.ZonePolicy;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZoneUseCaseImpl implements ZonePortIn {

    private final ZonePortOut zonePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ZonePolicy zonePolicy = new ZonePolicy();

    @Autowired
    public ZoneUseCaseImpl(
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            OrganizationAccessGuard organizationAccessGuard
    ) {
        this.zonePortOut = zonePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
    }

    // Kept for focused legacy unit tests that exercise only the domain policy.
    public ZoneUseCaseImpl(ZonePortOut zonePortOut) {
        this(zonePortOut, null, null);
    }

    @Override
    @Transactional
    public Zone createZone(Zone zone) {
        ensureCanConfigureParkingLot(zone.getParkingLotId());
        zonePolicy.initialize(zone);
        validateConfigurableParkingLot(zone.getParkingLotId());
        normalizeAndValidateVehicleTypes(zone);

        if (zonePortOut.existsByParkingLotIdAndCode(zone.getParkingLotId(), zone.getCode())) {
            throw new ConflictException("Zone code already exists in this parking lot");
        }

        zone.setZoneId(UUID.randomUUID());
        return zonePortOut.save(zone);
    }

    @Override
    @Transactional(readOnly = true)
    public Zone getZoneById(UUID zoneId) {
        return findExistingZone(zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Zone> getZones(UUID parkingLotId, UUID vehicleTypeId, ZoneStatus status, String keyword) {
        ensureCanListParkingLotResources(parkingLotId);
        return zonePortOut.findAll(parkingLotId, vehicleTypeId, status, normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public Zone updateZone(UUID zoneId, Zone zone) {
        Zone existingZone = getZoneById(zoneId);
        ensureCanConfigureParkingLot(existingZone.getParkingLotId());

        existingZone.setCode(zone.getCode());
        existingZone.setName(zone.getName());
        existingZone.setVehicleTypeId(zone.getVehicleTypeId());
        existingZone.setVehicleTypeIds(zone.getVehicleTypeIds());
        existingZone.setCapacity(zone.getCapacity());

        zonePolicy.initialize(existingZone);
        normalizeAndValidateVehicleTypes(existingZone);
        validateCapacity(existingZone);

        if (zonePortOut.existsByParkingLotIdAndCodeAndZoneIdNot(
                existingZone.getParkingLotId(),
                existingZone.getCode(),
                zoneId
        )) {
            throw new ConflictException("Zone code already exists in this parking lot");
        }

        return zonePortOut.save(existingZone);
    }

    @Override
    @Transactional
    public void deleteZone(UUID zoneId) {
        Zone existingZone = getZoneById(zoneId);
        ensureCanConfigureParkingLot(existingZone.getParkingLotId());

        if (existingZone.getStatus() == ZoneStatus.CLOSED) {
            return;
        }
        ensureNoActiveGates(zoneId);
        ensureNoOpenSessions(zoneId);

        zonePolicy.close(existingZone);
        zonePortOut.save(existingZone);
    }

    @Override
    @Transactional
    public Zone activateZone(UUID zoneId) {
        Zone existingZone = getZoneById(zoneId);
        ensureCanConfigureParkingLot(existingZone.getParkingLotId());

        validateConfigurableParkingLot(existingZone.getParkingLotId());
        normalizeAndValidateVehicleTypes(existingZone);

        zonePolicy.activate(existingZone);
        return zonePortOut.save(existingZone);
    }

    @Override
    @Transactional
    public Zone markZoneMaintenance(UUID zoneId) {
        Zone existingZone = getZoneById(zoneId);
        ensureCanConfigureParkingLot(existingZone.getParkingLotId());

        zonePolicy.markMaintenance(existingZone);
        return zonePortOut.save(existingZone);
    }

    @Override
    @Transactional
    public Zone closeZone(UUID zoneId) {
        Zone existingZone = getZoneById(zoneId);
        ensureCanConfigureParkingLot(existingZone.getParkingLotId());

        ensureNoActiveGates(zoneId);
        ensureNoOpenSessions(zoneId);

        zonePolicy.close(existingZone);
        return zonePortOut.save(existingZone);
    }

    private void validateConfigurableParkingLot(UUID parkingLotId) {
        if (!zonePortOut.existsConfigurableParkingLotById(parkingLotId)
                && !zonePortOut.existsActiveParkingLotById(parkingLotId)) {
            throw new NotFoundException("Configurable parking lot not found");
        }
    }

    private void validateVehicleType(UUID vehicleTypeId) {
        if (!zonePortOut.existsActiveVehicleTypeById(vehicleTypeId)) {
            throw new NotFoundException("Active vehicle type not found");
        }
    }

    private void validateCapacity(Zone zone) {
        long openSessions = zonePortOut.countOpenSessions(zone.getZoneId());
        if (zone.getCapacity() < openSessions) {
            throw new BadRequestException("capacity must not be less than current open sessions");
        }
    }

    private void ensureNoOpenSessions(UUID zoneId) {
        if (zonePortOut.hasOpenSessions(zoneId)) {
            throw new ConflictException("Zone has open parking sessions");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private void ensureNoActiveGates(UUID zoneId) {
        if (zonePortOut.hasActiveGates(zoneId)) {
            throw new ConflictException("Zone has active gates");
        }
    }

    /**
     * vehicleTypeId is retained temporarily for backward-compatible clients;
     * vehicleTypeIds is the authoritative list used for zoning and check-in.
     */
    private void normalizeAndValidateVehicleTypes(Zone zone) {
        Set<UUID> vehicleTypeIds = zone.getVehicleTypeIds() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(zone.getVehicleTypeIds());
        vehicleTypeIds.remove(null);
        if (vehicleTypeIds.isEmpty() && zone.getVehicleTypeId() != null) {
            vehicleTypeIds.add(zone.getVehicleTypeId());
        }
        if (vehicleTypeIds.isEmpty()) {
            throw new BadRequestException("Khu vực phải chọn ít nhất một loại xe được phép đỗ");
        }
        for (UUID vehicleTypeId : vehicleTypeIds) {
            validateVehicleType(vehicleTypeId);
        }
        zone.setVehicleTypeIds(vehicleTypeIds);
        zone.setVehicleTypeId(vehicleTypeIds.iterator().next());
    }

    private Zone findExistingZone(UUID zoneId) {
        Zone zone = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"));
        ensureCanAccessParkingLot(zone.getParkingLotId());
        return zone;
    }

    private void ensureCanListParkingLotResources(UUID parkingLotId) {
        if (organizationAccessGuard == null) {
            return;
        }
        if (parkingLotId == null && organizationAccessGuard.isCurrentParkingManager()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Parking manager must select an assigned parking lot"
            );
        }
        if (parkingLotId != null) {
            ensureCanAccessParkingLot(parkingLotId);
        }
    }

    private void ensureCanAccessParkingLot(UUID parkingLotId) {
        if (organizationAccessGuard == null || parkingLotPortOut == null) {
            return;
        }
        if (parkingLotId == null) {
            throw new NotFoundException("Parking lot not found");
        }
        organizationAccessGuard.ensureCanAccessParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
    }

    private void ensureCanConfigureParkingLot(UUID parkingLotId) {
        if (organizationAccessGuard == null || parkingLotPortOut == null) {
            return;
        }
        if (parkingLotId == null) {
            throw new NotFoundException("Parking lot not found");
        }
        organizationAccessGuard.ensureCanConfigureParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
    }
}
