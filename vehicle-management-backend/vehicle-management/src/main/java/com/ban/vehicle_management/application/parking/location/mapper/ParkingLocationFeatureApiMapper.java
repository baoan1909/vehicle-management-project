package com.ban.vehicle_management.application.parking.location.mapper;

import com.ban.vehicle_management.application.parking.location.model.ParkingLocationFeatureStatus;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response.ParkingMapFeatureResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLocationFeatureApiMapper {

    ParkingMapFeatureResponse toResponse(ParkingLocationFeatureStatus status);
}