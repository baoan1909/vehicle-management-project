package com.ban.vehicle_management.application.parking.parkingspace.usecase;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.in.ParkingSpacePortIn;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspace.policy.ParkingSpacePolicy;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingSpaceUseCaseImpl implements ParkingSpacePortIn {

    private final ParkingSpacePortOut parkingSpacePortOut;
    private final ZonePortOut zonePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingSpacePolicy parkingSpacePolicy = new ParkingSpacePolicy();

    @Autowired
    public ParkingSpaceUseCaseImpl(
            ParkingSpacePortOut parkingSpacePortOut,
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            OrganizationAccessGuard organizationAccessGuard
    ) {
        this.parkingSpacePortOut = parkingSpacePortOut;
        this.zonePortOut = zonePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
    }

    @Override
    @Transactional
    public ParkingSpace createParkingSpace(ParkingSpace parkingSpace) {
        Zone zone = findConfigurableZone(parkingSpace.getZoneId());
        if (zone.getTrackingMode() != TrackingMode.SPACE) {
            throw new BadRequestException("Zone is not in SPACE tracking mode");
        }
        parkingSpacePolicy.initialize(parkingSpace);
        if (parkingSpacePortOut.existsByZoneIdAndCode(zone.getZoneId(), parkingSpace.getCode())) {
            throw new ConflictException("Parking space code already exists in this zone");
        }
        parkingSpace.setParkingSpaceId(UUID.randomUUID());
        parkingSpace.setZoneId(zone.getZoneId());
        return parkingSpacePortOut.save(parkingSpace);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSpace getParkingSpaceById(UUID parkingSpaceId) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        ensureCanAccessZone(parkingSpace.getZoneId());
        return parkingSpace;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpace> getParkingSpaces(UUID zoneId, ParkingSpaceStatus status, String lifecycleStatus) {
        Zone zone = findReadableZone(zoneId);
        String lifecycle = lifecycleStatus != null ? lifecycleStatus : "ACTIVE";
        List<ParkingSpace> spaces = parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zone.getZoneId(), lifecycle);
        if (status == null) {
            return spaces;
        }
        return spaces.stream().filter(space -> space.getStatus() == status).toList();
    }

    @Override
    @Transactional
    public ParkingSpace updateParkingSpace(UUID parkingSpaceId, ParkingSpace parkingSpace) {
        ParkingSpace existing = getParkingSpaceOrThrow(parkingSpaceId);
        Zone zone = findConfigurableZone(existing.getZoneId());
        if (parkingSpace.getZoneId() != null && !parkingSpace.getZoneId().equals(existing.getZoneId())) {
            throw new BadRequestException("Parking space cannot be moved to another zone");
        }
        existing.setCode(parkingSpace.getCode());
        existing.setVehicleTypeId(parkingSpace.getVehicleTypeId());
        existing.setX(parkingSpace.getX());
        existing.setY(parkingSpace.getY());
        existing.setWidth(parkingSpace.getWidth());
        existing.setHeight(parkingSpace.getHeight());
        existing.setRotation(parkingSpace.getRotation());
        parkingSpacePolicy.validateState(existing);
        if (parkingSpacePortOut.existsByZoneIdAndCodeAndParkingSpaceIdNot(
                zone.getZoneId(), existing.getCode(), parkingSpaceId)) {
            throw new ConflictException("Parking space code already exists in this zone");
        }
        return parkingSpacePortOut.save(existing);
    }

    @Override
    @Transactional
    public void deleteParkingSpace(UUID parkingSpaceId) {
        ParkingSpace existing = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(existing.getZoneId());
        if (existing.getStatus() == ParkingSpaceStatus.OCCUPIED) {
            throw new ConflictException("Cannot archive occupied parking space");
        }
        parkingSpacePolicy.archive(existing);
        parkingSpacePortOut.save(existing);
    }

    @Override
    @Transactional
    public List<ParkingSpace> bulkCreateParkingSpaces(List<ParkingSpace> parkingSpaces) {
        if (parkingSpaces == null || parkingSpaces.isEmpty()) {
            throw new BadRequestException("No parking spaces to create");
        }
        Zone zone = findConfigurableZone(parkingSpaces.get(0).getZoneId());
        if (zone.getTrackingMode() != TrackingMode.SPACE) {
            throw new BadRequestException("Zone is not in SPACE tracking mode");
        }
        for (ParkingSpace space : parkingSpaces) {
            if (space.getZoneId() == null || !space.getZoneId().equals(zone.getZoneId())) {
                throw new BadRequestException("All spaces must belong to the same zone");
            }
            parkingSpacePolicy.initialize(space);
            if (parkingSpacePortOut.existsByZoneIdAndCode(zone.getZoneId(), space.getCode())) {
                throw new ConflictException("Parking space code already exists: " + space.getCode());
            }
            space.setParkingSpaceId(UUID.randomUUID());
            space.setZoneId(zone.getZoneId());
        }
        return parkingSpacePortOut.saveAll(parkingSpaces);
    }

    @Override
    @Transactional
    public ParkingSpace occupyParkingSpace(UUID parkingSpaceId, ManualOccupancyType occupancyType, String reason) {
        return setManualStatus(parkingSpaceId, ParkingSpaceStatus.OCCUPIED, occupancyType, reason);
    }

    @Override
    @Transactional
    public ParkingSpace releaseParkingSpace(UUID parkingSpaceId) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        parkingSpacePolicy.release(parkingSpace);
        return parkingSpacePortOut.save(parkingSpace);
    }

    @Override
    @Transactional
    public ParkingSpace reserveParkingSpace(UUID parkingSpaceId) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        parkingSpacePolicy.reserve(parkingSpace);
        return parkingSpacePortOut.save(parkingSpace);
    }

    @Override
    @Transactional
    public ParkingSpace markParkingSpaceMaintenance(UUID parkingSpaceId) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        parkingSpacePolicy.markMaintenance(parkingSpace);
        return parkingSpacePortOut.save(parkingSpace);
    }

    @Override
    @Transactional
    public ParkingSpace setManualStatus(UUID parkingSpaceId, ParkingSpaceStatus targetStatus,
            ManualOccupancyType occupancyType, String reason) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        parkingSpacePolicy.setManualStatus(parkingSpace, targetStatus, occupancyType, reason);
        return parkingSpacePortOut.save(parkingSpace);
    }

    @Override
    @Transactional
    public ParkingSpace setAutomaticStatus(UUID parkingSpaceId, ParkingSpaceStatus targetStatus,
            ManualOccupancyType occupancyType) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        parkingSpacePolicy.setAutomaticStatus(parkingSpace, targetStatus, occupancyType);
        return parkingSpacePortOut.save(parkingSpace);
    }

    @Override
    @Transactional
    public ParkingSpace archiveParkingSpace(UUID parkingSpaceId) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        parkingSpacePolicy.archive(parkingSpace);
        return parkingSpacePortOut.save(parkingSpace);
    }

    private Zone findReadableZone(UUID zoneId) {
        if (zoneId == null) {
            throw new NotFoundException("Zone not found");
        }
        Zone zone = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"));
        ensureCanAccessParkingLot(zone.getParkingLotId());
        return zone;
    }

    private Zone findConfigurableZone(UUID zoneId) {
        if (zoneId == null) {
            throw new NotFoundException("Zone not found");
        }
        Zone zone = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"));
        organizationAccessGuard.ensureCanConfigureParkingLot(getParkingLotOrThrow(zone.getParkingLotId()));
        return zone;
    }

    private void ensureCanAccessZone(UUID zoneId) {
        findReadableZone(zoneId);
    }

    private void ensureCanAccessParkingLot(UUID parkingLotId) {
        organizationAccessGuard.ensureCanAccessParkingLot(getParkingLotOrThrow(parkingLotId));
    }

    private com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot getParkingLotOrThrow(
            UUID parkingLotId) {
        if (parkingLotId == null) {
            throw new NotFoundException("Parking lot not found");
        }
        return parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
    }

    private ParkingSpace getParkingSpaceOrThrow(UUID parkingSpaceId) {
        return parkingSpacePortOut.findById(parkingSpaceId)
                .orElseThrow(() -> new NotFoundException("Parking space not found"));
    }
}
