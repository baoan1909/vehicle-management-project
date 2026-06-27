package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkingevent.port.out.ParkingEventPortOut;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingEventPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingEventEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingEventRepository;
import org.springframework.stereotype.Component;

@Component
public class ParkingEventPersistenceAdapter implements ParkingEventPortOut {

    private final ParkingEventRepository parkingEventRepository;
    private final ParkingEventPersistenceMapper parkingEventPersistenceMapper;

    public ParkingEventPersistenceAdapter(
            ParkingEventRepository parkingEventRepository,
            ParkingEventPersistenceMapper parkingEventPersistenceMapper
    ) {
        this.parkingEventRepository = parkingEventRepository;
        this.parkingEventPersistenceMapper = parkingEventPersistenceMapper;
    }

    @Override
    public ParkingEvent save(ParkingEvent parkingEvent) {
        ParkingEventEntity savedEntity = parkingEventRepository.saveAndFlush(
                parkingEventPersistenceMapper.toEntity(parkingEvent)
        );
        return parkingEventPersistenceMapper.toDomain(savedEntity);
    }
}
