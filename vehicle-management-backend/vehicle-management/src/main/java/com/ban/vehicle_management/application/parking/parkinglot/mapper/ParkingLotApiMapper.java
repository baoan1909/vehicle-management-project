package com.ban.vehicle_management.application.parking.parkinglot.mapper;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request.CreateParkingLotRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request.UpdateParkingLotRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response.ParkingLotAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingLotApiMapper {

    @Mapping(target = "parkingLotId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingLot toDomain(CreateParkingLotRequest request);

    @Mapping(target = "parkingLotId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingLot toDomain(UpdateParkingLotRequest request);

    ParkingLotAdminResponse toAdminResponse(ParkingLot parkingLot);

    List<ParkingLotAdminResponse> toAdminResponses(List<ParkingLot> parkingLots);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}