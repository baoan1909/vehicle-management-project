package com.ban.vehicle_management.application.parking.parkingsession.mapper;

import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingQrImageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingQrImageApiMapper {

    @Mapping(target = "qrPayload", source = "payload")
    ParkingQrImageResponse toResponse(String payload);
}
