package com.ban.vehicle_management.entrypoint.controller.parking;


import com.ban.vehicle_management.application.parking.lane.mapper.LaneApiMapper;
import com.ban.vehicle_management.application.parking.lane.port.in.LanePortIn;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.entrypoint.dto.parking.lane.request.CreateLaneRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.lane.request.LaneFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.lane.request.UpdateLaneRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.lane.response.LaneResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parking/lanes")
public class LaneController {

    private final LanePortIn lanePortIn;
    private final LaneApiMapper laneApiMapper;

    public  LaneController(LanePortIn lanePortIn, LaneApiMapper laneApiMapper){
        this.lanePortIn = lanePortIn;
        this.laneApiMapper = laneApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LaneResponse>> createLane(@RequestBody CreateLaneRequest request){
        Lane createdLane = lanePortIn.createLane(laneApiMapper.toDomain(request));
        return  ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Lane created successfully", laneApiMapper.toAdminResponse(createdLane)));
    }

    @GetMapping("/{laneId}")
    public ResponseEntity<ApiResponse<LaneResponse>> getLaneById(@PathVariable UUID laneId){
        Lane lane = lanePortIn.getLaneById(laneId);
        return ResponseEntity.ok(ApiResponse.ok("Fetched lane successfully", laneApiMapper.toAdminResponse(lane)));

    }

    @GetMapping
    public  ResponseEntity<ApiResponse<List<LaneResponse>>> getLanes(@ModelAttribute LaneFilterRequest request){
        List<Lane> lanes = lanePortIn.getLanes(
                request.gateId(),
                request.direction(),
                request.status(),
                request.keyword()
        );
        return  ResponseEntity.ok(ApiResponse.ok("Fetched lanes successfully", laneApiMapper.toAdminResponses(lanes)));
    }

    @PutMapping("/{laneId}")
    public ResponseEntity<ApiResponse<LaneResponse>> updateLane(
            @PathVariable UUID laneId,
            @RequestBody UpdateLaneRequest request
    ) {
        Lane updatedLane = lanePortIn.updateLane(laneId, laneApiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok("Lane updated successfully", laneApiMapper.toAdminResponse(updatedLane)));
    }

    @DeleteMapping("/{laneId}")
    public ResponseEntity<ApiResponse<Void>> deleteLane(@PathVariable UUID laneId) {
        lanePortIn.deleteLane(laneId);
        return ResponseEntity.ok(ApiResponse.ok("Lane closed successfully"));
    }

    @PatchMapping("/{laneId}/activate")
    public ResponseEntity<ApiResponse<LaneResponse>> activateLane(@PathVariable UUID laneId) {
        Lane lane = lanePortIn.activateLane(laneId);
        return ResponseEntity.ok(ApiResponse.ok("Lane activated successfully", laneApiMapper.toAdminResponse(lane)));
    }

    @PatchMapping("/{laneId}/maintenance")
    public ResponseEntity<ApiResponse<LaneResponse>> markLaneMaintenance(@PathVariable UUID laneId) {
        Lane lane = lanePortIn.markLaneMaintenance(laneId);
        return ResponseEntity.ok(ApiResponse.ok("Lane marked as maintenance successfully", laneApiMapper.toAdminResponse(lane)));
    }

    @PatchMapping("/{laneId}/force-maintenance")
    public ResponseEntity<ApiResponse<LaneResponse>> forceLaneMaintenance(@PathVariable UUID laneId) {
        Lane lane = lanePortIn.forceLaneMaintenance(laneId);
        return ResponseEntity.ok(ApiResponse.ok("Lane force maintenance successfully", laneApiMapper.toAdminResponse(lane)));
    }

    @PatchMapping("/{laneId}/close")
    public ResponseEntity<ApiResponse<LaneResponse>> closeLane(@PathVariable UUID laneId) {
        Lane lane = lanePortIn.closeLane(laneId);
        return ResponseEntity.ok(ApiResponse.ok("Lane closed successfully", laneApiMapper.toAdminResponse(lane)));
    }
}
