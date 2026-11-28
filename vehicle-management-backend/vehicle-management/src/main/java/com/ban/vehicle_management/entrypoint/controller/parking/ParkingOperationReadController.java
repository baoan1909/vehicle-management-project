package com.ban.vehicle_management.entrypoint.controller.parking;
import com.ban.vehicle_management.application.parking.operation.port.in.ParkingOperationReadPortIn;
import com.ban.vehicle_management.application.parking.operation.mapper.ParkingOperationApiMapper;
import com.ban.vehicle_management.application.parking.parkingsummary.mapper.SpaceSummaryApiMapper;
import com.ban.vehicle_management.application.parking.parkinglayout.mapper.ParkingLayoutApiMapper;
import com.ban.vehicle_management.entrypoint.dto.parking.operation.response.OperationLevelResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.operation.response.OperationZoneResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.operation.response.OperationLaneResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.operation.response.OperationSpaceResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.operation.response.OperationLevelMapResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response.ParkingSpaceSummaryResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.PublicReservationLayoutResponse;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
@RestController
@RequestMapping("/api/parking/operations")
@RequiredArgsConstructor
@PreAuthorize("@permissionAuthorizer.hasAnyPermission('PARKING_SPACE_READ_ALL', 'PARKING_SESSION_CHECK_IN_ALL', 'PARKING_SESSION_CHECK_OUT_ALL')")
public class ParkingOperationReadController {
    private final ParkingOperationReadPortIn readPort;
    private final ParkingOperationApiMapper mapper;
    private final SpaceSummaryApiMapper summaryMapper;
    private final ParkingLayoutApiMapper layoutMapper;

    @GetMapping("/lots/{lotId}/levels")
    public ApiResponse<List<OperationLevelResponse>> levels(@PathVariable UUID lotId) {
        return ApiResponse.ok("Fetched operation levels", mapper.toLevels(readPort.getLevels(lotId)));
    }
    @GetMapping("/lots/{lotId}/zones")
    public ApiResponse<List<OperationZoneResponse>> zones(@PathVariable UUID lotId,
            @RequestParam(required = false) ZoneStatus status) {
        return ApiResponse.ok("Fetched operation zones", mapper.toZones(readPort.getZones(lotId, status)));
    }
    @GetMapping("/lots/{lotId}/lanes")
    public ApiResponse<List<OperationLaneResponse>> lanes(@PathVariable UUID lotId,
            @RequestParam(required = false) LaneDirection direction) {
        return ApiResponse.ok("Fetched operation lanes", mapper.toLanes(readPort.getLanes(lotId, direction)));
    }
    @GetMapping("/lots/{lotId}/summary")
    public ApiResponse<ParkingSpaceSummaryResponse> summary(@PathVariable UUID lotId) {
        return ApiResponse.ok("Fetched operation summary", summaryMapper.toResponse(readPort.getLotSummary(lotId)));
    }
    @GetMapping("/levels/{levelId}/map")
    public ApiResponse<OperationLevelMapResponse> map(@PathVariable UUID levelId) {
        return ApiResponse.ok("Fetched operation map", mapper.toMap(readPort.getLevelMap(levelId)));
    }
    @GetMapping("/zones/{zoneId}/spaces")
    public ApiResponse<List<OperationSpaceResponse>> spaces(@PathVariable UUID zoneId) {
        return ApiResponse.ok("Fetched operation spaces", mapper.toSpaces(readPort.getSpaces(zoneId)));
    }
    @GetMapping("/zones/{zoneId}/layout/published")
    public ApiResponse<PublicReservationLayoutResponse> published(@PathVariable UUID zoneId) {
        return ApiResponse.ok("Fetched published operation layout",
                layoutMapper.toPublicReservationResponse(readPort.getPublishedLayout(zoneId)));
    }
    @GetMapping("/zones/{zoneId}/allocated-space-ids")
    public ApiResponse<List<UUID>> allocatedSpaces(@PathVariable UUID zoneId) {
        return ApiResponse.ok("Fetched allocated space identifiers", readPort.getAllocatedSpaceIds(zoneId));
    }
}
