package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingLotPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.parking.ParkingLotSpecifications;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ParkingLotPersistenceAdapter implements ParkingLotPortOut {

    private final ParkingLotRepository parkingLotRepository;
    private final ParkingLotPersistenceMapper parkingLotPersistenceMapper;

    public ParkingLotPersistenceAdapter(
            ParkingLotRepository parkingLotRepository,
            ParkingLotPersistenceMapper parkingLotPersistenceMapper
    ) {
        this.parkingLotRepository = parkingLotRepository;
        this.parkingLotPersistenceMapper = parkingLotPersistenceMapper;
    }

    @Override
    public ParkingLot save(ParkingLot parkingLot) {
        ParkingLotEntity savedEntity = parkingLotRepository.saveAndFlush(
                parkingLotPersistenceMapper.toEntity(parkingLot)
        );
        return parkingLotPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ParkingLot> findById(UUID parkingLotId) {
        return parkingLotRepository.findById(parkingLotId)
                .map(parkingLotPersistenceMapper::toDomain);
    }

    @Override
    public List<ParkingLot> findAll(ParkingLotStatus status, String keyword) {
        return parkingLotRepository.findAll(
                        ParkingLotSpecifications.withFilters(status, keyword)
                )
                .stream()
                .map(parkingLotPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return parkingLotRepository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeAndParkingLotIdNot(String code, UUID parkingLotId) {
        return parkingLotRepository.existsByCodeAndParkingLotIdNot(code, parkingLotId);
    }
}