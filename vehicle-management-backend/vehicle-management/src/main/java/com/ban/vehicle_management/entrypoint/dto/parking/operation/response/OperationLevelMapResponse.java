package com.ban.vehicle_management.entrypoint.dto.parking.operation.response;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response.ParkingSpaceSummaryResponse;
import java.util.List;
public record OperationLevelMapResponse(OperationLevelResponse level, List<OperationZoneResponse> zones,
        List<OperationSpaceResponse> spaces, ParkingSpaceSummaryResponse summary) {}
