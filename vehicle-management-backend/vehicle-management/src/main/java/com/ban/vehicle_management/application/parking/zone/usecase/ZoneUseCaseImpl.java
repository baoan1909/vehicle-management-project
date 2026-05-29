package com.ban.vehicle_management.application.parking.zone.usecase;

import com.ban.vehicle_management.application.parking.zone.port.in.ZonePortIn;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.parking.zone.policy.ZonePolicy;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZoneUseCaseImpl implements ZonePortIn {

    private final ZonePortOut zonePortOut;
    private final ZonePolicy zonePolicy = new ZonePolicy();

    public ZoneUseCaseImpl(ZonePortOut zonePortOut) {
        this.zonePortOut = zonePortOut;
    }

    @Override
    @Transactional
    public Zone createZone(Zone zone) {
        zonePolicy.initialize(zone);
        validateActiveParkingLot(zone.getParkingLotId());
        validateVehicleType(zone.getVehicleTypeId());

        if (zonePortOut.existsByParkingLotIdAndCode(zone.getParkingLotId(), zone.getCode())) {
            throw new ConflictException("Zone code already exists in this parking lot");
        }

        zone.setZoneId(UUID.randomUUID());
        return zonePortOut.save(zone);
    }

    @Override
    @Transactional(readOnly = true)
    public Zone getZoneById(UUID zoneId) {
        return zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Zone> getZones(UUID parkingLotId, UUID vehicleTypeId, ZoneStatus status, String keyword) {
        return zonePortOut.findAll(parkingLotId, vehicleTypeId, status, normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public Zone updateZone(UUID zoneId, Zone zone) {
        Zone existingZone = getZoneById(zoneId);

        existingZone.setCode(zone.getCode());
        existingZone.setName(zone.getName());
        existingZone.setVehicleTypeId(zone.getVehicleTypeId());
        existingZone.setCapacity(zone.getCapacity());

        zonePolicy.initialize(existingZone);
        validateVehicleType(existingZone.getVehicleTypeId());
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

        if (existingZone.getStatus() == ZoneStatus.CLOSED) {
            return;
        }

        ensureNoOpenSessions(zoneId);

        zonePolicy.close(existingZone);
        zonePortOut.save(existingZone);
    }

    @Override
    @Transactional
    public Zone activateZone(UUID zoneId) {
        Zone existingZone = getZoneById(zoneId);

        validateActiveParkingLot(existingZone.getParkingLotId());
        validateVehicleType(existingZone.getVehicleTypeId());

        zonePolicy.activate(existingZone);
        return zonePortOut.save(existingZone);
    }

    @Override
    @Transactional
    public Zone markZoneMaintenance(UUID zoneId) {
        Zone existingZone = getZoneById(zoneId);

        zonePolicy.markMaintenance(existingZone);
        return zonePortOut.save(existingZone);
    }

    @Override
    @Transactional
    public Zone closeZone(UUID zoneId) {
        Zone existingZone = getZoneById(zoneId);

        ensureNoOpenSessions(zoneId);

        zonePolicy.close(existingZone);
        return zonePortOut.save(existingZone);
    }

    private void validateActiveParkingLot(UUID parkingLotId) {
        if (!zonePortOut.existsActiveParkingLotById(parkingLotId)) {
            throw new NotFoundException("Active parking lot not found");
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
}