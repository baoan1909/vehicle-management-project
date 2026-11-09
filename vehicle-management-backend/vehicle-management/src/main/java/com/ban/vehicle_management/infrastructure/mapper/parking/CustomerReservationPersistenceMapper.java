package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.CustomerReservationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerReservationPersistenceMapper {
    CustomerReservation toDomain(CustomerReservationEntity entity);
    CustomerReservationEntity toEntity(CustomerReservation domain);
}
