package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import com.ban.vehicle_management.application.parking.parkinglot.port.in.ParkingLotPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkinglot.policy.ParkingLotPolicy;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingLotUseCaseImpl implements ParkingLotPortIn {

    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingLotPolicy parkingLotPolicy = new ParkingLotPolicy();

    public ParkingLotUseCaseImpl(ParkingLotPortOut parkingLotPortOut) {
        this.parkingLotPortOut = parkingLotPortOut;
    }

    @Override
    @Transactional
    public ParkingLot createParkingLot(ParkingLot parkingLot) {
        parkingLotPolicy.initialize(parkingLot);

        if (parkingLotPortOut.existsByCode(parkingLot.getCode())) {
            throw new ConflictException("Parking lot code already exists");
        }

        parkingLot.setParkingLotId(UUID.randomUUID());
        return parkingLotPortOut.save(parkingLot);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingLot getParkingLotById(UUID parkingLotId) {
        return parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLot> getParkingLots(ParkingLotStatus status, String keyword) {
        return parkingLotPortOut.findAll(status, normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public ParkingLot updateParkingLot(UUID parkingLotId, ParkingLot parkingLot) {
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);

        existingParkingLot.setCode(parkingLot.getCode());
        existingParkingLot.setName(parkingLot.getName());
        existingParkingLot.setAddress(parkingLot.getAddress());
        existingParkingLot.setTotalCapacity(parkingLot.getTotalCapacity());

        parkingLotPolicy.initialize(existingParkingLot);

        if (parkingLotPortOut.existsByCodeAndParkingLotIdNot(existingParkingLot.getCode(), parkingLotId)) {
            throw new ConflictException("Parking lot code already exists");
        }

        return parkingLotPortOut.save(existingParkingLot);
    }

    @Override
    @Transactional
    public void deleteParkingLot(UUID parkingLotId) {
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);

        if (existingParkingLot.getStatus() == ParkingLotStatus.CLOSED) {
            return;
        }

        parkingLotPolicy.close(existingParkingLot);
        parkingLotPortOut.save(existingParkingLot);
    }

    @Override
    @Transactional
    public ParkingLot activateParkingLot(UUID parkingLotId) {
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);

        parkingLotPolicy.activate(existingParkingLot);
        return parkingLotPortOut.save(existingParkingLot);
    }

    @Override
    @Transactional
    public ParkingLot markParkingLotMaintenance(UUID parkingLotId) {
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);

        parkingLotPolicy.markMaintenance(existingParkingLot);
        return parkingLotPortOut.save(existingParkingLot);
    }

    @Override
    @Transactional
    public ParkingLot closeParkingLot(UUID parkingLotId) {
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);

        parkingLotPolicy.close(existingParkingLot);
        return parkingLotPortOut.save(existingParkingLot);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}