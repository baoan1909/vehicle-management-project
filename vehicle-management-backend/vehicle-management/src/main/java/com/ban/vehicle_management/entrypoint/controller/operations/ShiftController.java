package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.shift.mapper.ShiftApiMapper;
import com.ban.vehicle_management.application.operations.shift.port.in.ShiftPortIn;
import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.entrypoint.dto.operations.shift.request.ApproveWorkScheduleRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shift.request.CancelShiftRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shift.request.CloseShiftRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shift.request.GenerateWorkScheduleRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shift.request.OpenShiftRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shift.request.ShiftFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shift.response.ShiftAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations")
public class ShiftController {

    private final ShiftPortIn shiftPortIn;
    private final ShiftApiMapper apiMapper;

    public ShiftController(
            ShiftPortIn shiftPortIn,
            ShiftApiMapper apiMapper
    ) {
        this.shiftPortIn = shiftPortIn;
        this.apiMapper = apiMapper;
    }

    @PostMapping("/work-schedules/generate-week")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_CREATE_ALL')"
    )
    public ResponseEntity<
            ApiResponse<List<ShiftAdminResponse>>
            > generateWeek(
            @RequestBody GenerateWorkScheduleRequest request
    ) {
        List<Shift> results = shiftPortIn.generateWeek(
                request.parkingLotId(),
                request.weekStartDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(
                        "Weekly work schedule generated successfully",
                        apiMapper.toAdminResponses(results)
                )
        );
    }

    @PatchMapping("/work-schedules/approve-week")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_UPDATE_ALL')"
    )
    public ResponseEntity<
            ApiResponse<List<ShiftAdminResponse>>
            > approveWeek(
            @RequestBody ApproveWorkScheduleRequest request
    ) {
        List<Shift> results = shiftPortIn.approveWeek(
                request.parkingLotId(),
                request.weekStartDate()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Weekly work schedule approved successfully",
                apiMapper.toAdminResponses(results)
        ));
    }

    @GetMapping("/shifts/{shiftId}")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_READ_ALL')"
    )
    public ResponseEntity<ApiResponse<ShiftAdminResponse>> getById(
            @PathVariable UUID shiftId
    ) {
        Shift shift = shiftPortIn.getShiftById(shiftId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched shift successfully",
                apiMapper.toAdminResponse(shift)
        ));
    }

    @GetMapping("/shifts")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_READ_ALL')"
    )
    public ResponseEntity<
            ApiResponse<List<ShiftAdminResponse>>
            > getAll(
            @ModelAttribute ShiftFilterRequest request
    ) {
        List<Shift> results = shiftPortIn.getShifts(
                request.parkingLotId(),
                request.fromDate(),
                request.toDate(),
                request.shiftType(),
                request.status(),
                request.employeeId(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched shifts successfully",
                apiMapper.toAdminResponses(results)
        ));
    }

    @PatchMapping("/shifts/{shiftId}/open")
    @PreAuthorize("""
            @permissionAuthorizer.hasPermission('SHIFT_UPDATE_ALL')
            or
            @permissionAuthorizer.hasPermission('SHIFT_OPEN_OWN')
            """)
    public ResponseEntity<ApiResponse<ShiftAdminResponse>> open(
            @PathVariable UUID shiftId,
            @RequestBody OpenShiftRequest request
    ) {
        Shift result = shiftPortIn.openShift(
                shiftId,
                request.openingCash(),
                request.note()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Shift opened successfully",
                apiMapper.toAdminResponse(result)
        ));
    }

    @PatchMapping("/shifts/{shiftId}/close")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_UPDATE_ALL')"
    )
    public ResponseEntity<ApiResponse<ShiftAdminResponse>> close(
            @PathVariable UUID shiftId,
            @RequestBody CloseShiftRequest request
    ) {
        Shift result = shiftPortIn.closeShift(
                shiftId,
                request.closingCash(),
                request.note()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Shift closed successfully",
                apiMapper.toAdminResponse(result)
        ));
    }

    @PatchMapping("/shifts/{shiftId}/cancel")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_UPDATE_ALL')"
    )
    public ResponseEntity<ApiResponse<ShiftAdminResponse>> cancel(
            @PathVariable UUID shiftId,
            @RequestBody CancelShiftRequest request
    ) {
        Shift result = shiftPortIn.cancelShift(
                shiftId,
                request.reason()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Shift cancelled successfully",
                apiMapper.toAdminResponse(result)
        ));
    }
}