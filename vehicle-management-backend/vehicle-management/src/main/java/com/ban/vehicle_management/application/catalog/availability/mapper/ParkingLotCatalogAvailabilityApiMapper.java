package com.ban.vehicle_management.application.catalog.availability.mapper;

import com.ban.vehicle_management.application.catalog.availability.model.ParkingLotCatalogAvailability;
import com.ban.vehicle_management.entrypoint.dto.catalog.availability.response.ParkingLotCatalogAvailabilityResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLotCatalogAvailabilityApiMapper {
    ParkingLotCatalogAvailabilityResponse toResponse(ParkingLotCatalogAvailability availability);
}
