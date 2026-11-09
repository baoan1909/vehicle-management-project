package com.ban.vehicle_management.application.parking.parkingspace.mapper;

import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request.CreateParkingSpaceRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request.UpdateParkingSpaceRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response.ParkingSpaceAdminResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.IterableMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.Named;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ParkingSpaceApiMapper {

    @Mapping(target = "parkingSpaceId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lifecycleStatus", ignore = true)
    @Mapping(target = "statusSource", ignore = true)
    @Mapping(target = "manualOccupancyType", ignore = true)
    @Mapping(target = "statusReason", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingSpace toDomain(CreateParkingSpaceRequest request);

    @Mapping(target = "parkingSpaceId", ignore = true)
    @Mapping(target = "zoneId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lifecycleStatus", ignore = true)
    @Mapping(target = "statusSource", ignore = true)
    @Mapping(target = "manualOccupancyType", ignore = true)
    @Mapping(target = "statusReason", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Named("createWithZone")
    ParkingSpace toDomain(CreateParkingSpaceRequest request, @Context UUID zoneId);

    @IterableMapping(
            qualifiedByName = "createWithZone",
            nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<ParkingSpace> toDomains(List<CreateParkingSpaceRequest> requests, @Context UUID zoneId);

    @AfterMapping
    default void applyZone(@MappingTarget ParkingSpace parkingSpace, @Context UUID zoneId) {
        parkingSpace.setZoneId(zoneId);
    }

    @Mapping(target = "parkingSpaceId", ignore = true)
    @Mapping(target = "zoneId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lifecycleStatus", ignore = true)
    @Mapping(target = "statusSource", ignore = true)
    @Mapping(target = "manualOccupancyType", ignore = true)
    @Mapping(target = "statusReason", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingSpace toDomain(UpdateParkingSpaceRequest request);

    ParkingSpaceAdminResponse toAdminResponse(ParkingSpace parkingSpace);

    List<ParkingSpaceAdminResponse> toAdminResponses(List<ParkingSpace> parkingSpaces);
}
