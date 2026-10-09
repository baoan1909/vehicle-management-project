package com.ban.vehicle_management.application.parking.parkinglevel.usecase;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.in.ParkingLevelPortIn;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkinglevel.policy.ParkingLevelPolicy;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingLevelUseCaseImpl implements ParkingLevelPortIn {

    private final ParkingLevelPortOut parkingLevelPortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingLevelPolicy parkingLevelPolicy = new ParkingLevelPolicy();

    @Autowired
    public ParkingLevelUseCaseImpl(
            ParkingLevelPortOut parkingLevelPortOut,
            ParkingLotPortOut parkingLotPortOut,
            OrganizationAccessGuard organizationAccessGuard
    ) {
        this.parkingLevelPortOut = parkingLevelPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
    }

    @Override
    @Transactional
    public ParkingLevel createParkingLevel(ParkingLevel parkingLevel) {
        ensureCanConfigureParkingLot(parkingLevel.getParkingLotId());
        parkingLevelPolicy.initialize(parkingLevel);
        if (parkingLevelPortOut.existsByParkingLotIdAndCode(
                parkingLevel.getParkingLotId(), parkingLevel.getCode())) {
            throw new ConflictException("Parking level code already exists in this parking lot");
        }
        parkingLevel.setParkingLevelId(UUID.randomUUID());
        return parkingLevelPortOut.save(parkingLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingLevel getParkingLevelById(UUID parkingLevelId) {
        ParkingLevel parkingLevel = parkingLevelPortOut.findById(parkingLevelId)
                .orElseThrow(() -> new NotFoundException("Parking level not found"));
        ensureCanAccessParkingLot(parkingLevel.getParkingLotId());
        return parkingLevel;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLevel> getParkingLevels(UUID parkingLotId, ParkingLevelStatus status, String keyword) {
        ensureCanAccessParkingLot(parkingLotId);
        return parkingLevelPortOut.findAll(parkingLotId, status, keyword);
    }

    @Override
    @Transactional
    public ParkingLevel updateParkingLevel(UUID parkingLevelId, ParkingLevel parkingLevel) {
        ParkingLevel existing = getParkingLevelById(parkingLevelId);
        ensureCanConfigureParkingLot(existing.getParkingLotId());
        if (parkingLevel.getParkingLotId() != null
                && !parkingLevel.getParkingLotId().equals(existing.getParkingLotId())) {
            throw new BadRequestException("Parking level cannot be moved to another parking lot");
        }
        existing.setCode(parkingLevel.getCode());
        existing.setName(parkingLevel.getName());
        existing.setDisplayOrder(parkingLevel.getDisplayOrder());
        existing.setElevation(parkingLevel.getElevation());
        existing.setFloorHeight(parkingLevel.getFloorHeight());
        existing.setCanvasWidth(parkingLevel.getCanvasWidth());
        existing.setCanvasHeight(parkingLevel.getCanvasHeight());
        parkingLevelPolicy.validateState(existing);
        if (parkingLevelPortOut.existsByParkingLotIdAndCodeAndParkingLevelIdNot(
                existing.getParkingLotId(), existing.getCode(), parkingLevelId)) {
            throw new ConflictException("Parking level code already exists in this parking lot");
        }
        return parkingLevelPortOut.save(existing);
    }

    @Override
    @Transactional
    public void deleteParkingLevel(UUID parkingLevelId) {
        ParkingLevel existing = getParkingLevelById(parkingLevelId);
        ensureCanConfigureParkingLot(existing.getParkingLotId());
        if (parkingLevelPortOut.countZonesByParkingLevelId(parkingLevelId) > 0) {
            throw new ConflictException("Parking level still has zones");
        }
        parkingLevelPortOut.delete(parkingLevelId);
    }

    @Override
    @Transactional
    public ParkingLevel activateParkingLevel(UUID parkingLevelId) {
        ParkingLevel parkingLevel = getParkingLevelById(parkingLevelId);
        ensureCanConfigureParkingLot(parkingLevel.getParkingLotId());
        parkingLevelPolicy.activate(parkingLevel);
        return parkingLevelPortOut.save(parkingLevel);
    }

    @Override
    @Transactional
    public ParkingLevel markParkingLevelMaintenance(UUID parkingLevelId) {
        ParkingLevel parkingLevel = getParkingLevelById(parkingLevelId);
        ensureCanConfigureParkingLot(parkingLevel.getParkingLotId());
        parkingLevelPolicy.markMaintenance(parkingLevel);
        return parkingLevelPortOut.save(parkingLevel);
    }

    @Override
    @Transactional
    public ParkingLevel closeParkingLevel(UUID parkingLevelId) {
        ParkingLevel parkingLevel = getParkingLevelById(parkingLevelId);
        ensureCanConfigureParkingLot(parkingLevel.getParkingLotId());
        parkingLevelPolicy.close(parkingLevel);
        return parkingLevelPortOut.save(parkingLevel);
    }

    private void ensureCanAccessParkingLot(UUID parkingLotId) {
        if (parkingLotId == null) {
            throw new NotFoundException("Parking lot not found");
        }
        organizationAccessGuard.ensureCanAccessParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
    }

    private void ensureCanConfigureParkingLot(UUID parkingLotId) {
        if (parkingLotId == null) {
            throw new NotFoundException("Parking lot not found");
        }
        organizationAccessGuard.ensureCanConfigureParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
    }
}
