package com.ban.vehicle_management.application.parking.parkinglevel.mapper;

import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.request.CreateParkingLevelRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.request.UpdateParkingLevelRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.response.ParkingLevelAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.response.LevelMapResponse;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.LevelMapResult;
import com.ban.vehicle_management.application.parking.parkingspace.mapper.ParkingSpaceApiMapper;
import com.ban.vehicle_management.application.parking.parkingsummary.mapper.SpaceSummaryApiMapper;
import com.ban.vehicle_management.application.parking.zone.mapper.ZoneApiMapper;
import java.util.UUID;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        uses = {ZoneApiMapper.class, ParkingSpaceApiMapper.class, SpaceSummaryApiMapper.class})
public interface ParkingLevelApiMapper {

    @Mapping(target = "parkingLevelId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingLevel toDomain(CreateParkingLevelRequest request);

    @Mapping(target = "parkingLevelId", ignore = true)
    @Mapping(target = "parkingLotId", source = "parkingLotId")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingLevel toDomain(CreateParkingLevelRequest request, UUID parkingLotId);

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

    LevelMapResponse toLevelMapResponse(LevelMapResult result);
}
