package com.ban.vehicle_management.application.parking.parkingspaceallocation.mapper;

import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspaceallocation.request.CreateParkingSpaceAllocationRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspaceallocation.response.ParkingSpaceAllocationResponse;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingSpaceAllocationApiMapper {

    @Mapping(target = "allocationId", ignore = true)
    @Mapping(target = "allocationStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingSpaceAllocation toDomain(CreateParkingSpaceAllocationRequest request, UUID parkingSpaceId);

    ParkingSpaceAllocationResponse toResponse(ParkingSpaceAllocation allocation);

    List<ParkingSpaceAllocationResponse> toResponses(List<ParkingSpaceAllocation> allocations);
}
