package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.parkingsessionprepayment.model.ParkingSessionPrepayment;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionPrepaymentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingSessionPrepaymentPersistenceMapper {
    ParkingSessionPrepayment toDomain(ParkingSessionPrepaymentEntity entity);
    ParkingSessionPrepaymentEntity toEntity(ParkingSessionPrepayment domain);
}
