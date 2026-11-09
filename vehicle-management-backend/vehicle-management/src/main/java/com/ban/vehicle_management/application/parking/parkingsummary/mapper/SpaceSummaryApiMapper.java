package com.ban.vehicle_management.application.parking.parkingsummary.mapper;

import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceSummaryResult;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.PublicAvailabilityResult;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response.ParkingSpaceSummaryResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsummary.response.PublicAvailabilityResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.IterableMapping;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(componentModel = "spring")
public interface SpaceSummaryApiMapper {

    ParkingSpaceSummaryResponse toResponse(SpaceSummaryResult result);

    List<ParkingSpaceSummaryResponse> toResponses(List<SpaceSummaryResult> results);

    PublicAvailabilityResponse toResponse(PublicAvailabilityResult result);

    PublicAvailabilityResponse.VehicleTypeAvailabilityResponse toAvailabilityResponse(
            PublicAvailabilityResult.VehicleTypeAvailability result
    );

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<PublicAvailabilityResponse.VehicleTypeAvailabilityResponse> toAvailabilityResponses(
            List<PublicAvailabilityResult.VehicleTypeAvailability> results
    );
}
