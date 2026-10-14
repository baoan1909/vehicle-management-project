package com.ban.vehicle_management.application.parking.parkinglevel.mapper;

import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.request.CreateParkingLevelRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.request.UpdateParkingLevelRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.response.ParkingLevelAdminResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingLevelApiMapper {

    @Mapping(target = "parkingLevelId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingLevel toDomain(CreateParkingLevelRequest request);

    @Mapping(target = "parkingLevelId", ignore = true)
    @Mapping(target = "parkingLotId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingLevel toDomain(UpdateParkingLevelRequest request);

    ParkingLevelAdminResponse toAdminResponse(ParkingLevel parkingLevel);

    List<ParkingLevelAdminResponse> toAdminResponses(List<ParkingLevel> parkingLevels);
}
