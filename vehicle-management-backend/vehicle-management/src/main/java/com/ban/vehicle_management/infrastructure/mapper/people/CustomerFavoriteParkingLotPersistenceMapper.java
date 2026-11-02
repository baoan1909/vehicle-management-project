package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.customerfavoriteparkinglot.model.CustomerFavoriteParkingLot;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerFavoriteParkingLotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerFavoriteParkingLotPersistenceMapper {

    @Mapping(target = "id.customerId", source = "customerId")
    @Mapping(target = "id.parkingLotId", source = "parkingLotId")
    CustomerFavoriteParkingLotEntity toEntity(CustomerFavoriteParkingLot domain);

    @Mapping(target = "customerId", source = "id.customerId")
    @Mapping(target = "parkingLotId", source = "id.parkingLotId")
    CustomerFavoriteParkingLot toDomain(CustomerFavoriteParkingLotEntity entity);
}

