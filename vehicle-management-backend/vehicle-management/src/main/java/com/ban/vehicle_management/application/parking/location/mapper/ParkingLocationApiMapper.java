package com.ban.vehicle_management.application.parking.location.mapper;

import com.ban.vehicle_management.application.parking.location.model.ParkingLocationSearchResult;
import com.ban.vehicle_management.entrypoint.dto.parking.location.response.ParkingLocationResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLocationApiMapper {
    ParkingLocationResponse toResponse(ParkingLocationSearchResult result);

    List<ParkingLocationResponse> toResponses(List<ParkingLocationSearchResult> results);
}
