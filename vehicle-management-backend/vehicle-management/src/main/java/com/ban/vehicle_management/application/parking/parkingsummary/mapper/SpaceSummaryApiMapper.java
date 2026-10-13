package com.ban.vehicle_management.application.parking.parkingsummary.mapper;

import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceSummaryResult;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response.ParkingSpaceSummaryResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SpaceSummaryApiMapper {

    ParkingSpaceSummaryResponse toResponse(SpaceSummaryResult result);

    List<ParkingSpaceSummaryResponse> toResponses(List<SpaceSummaryResult> results);
}
