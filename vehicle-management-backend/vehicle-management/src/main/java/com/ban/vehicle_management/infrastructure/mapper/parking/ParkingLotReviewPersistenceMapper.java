package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.parkinglotreview.model.ParkingLotReview;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingLotReviewPersistenceMapper {
    @Mapping(target = "parkingLot", ignore = true)
    @Mapping(target = "customer", ignore = true)
    ParkingLotReviewEntity toEntity(ParkingLotReview domain);

    ParkingLotReview toDomain(ParkingLotReviewEntity entity);
}
