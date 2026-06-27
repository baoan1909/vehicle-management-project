package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.shiftassignment.mapper.ShiftAssignmentApiMapper;
import com.ban.vehicle_management.application.operations.shiftassignment.port.in.ShiftAssignmentPortIn;
import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request.CreateShiftAssignmentRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request.MyShiftAssignmentFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request.ReplaceShiftAssignmentRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request.ShiftAssignmentFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request.SwapShiftAssignmentRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request.UpdateShiftAssignmentRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.response.ShiftAssignmentAdminResponse;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations")
public class ShiftAssignmentController {

    private final ShiftAssignmentPortIn assignmentPortIn;
    private final ShiftAssignmentApiMapper apiMapper;

    public ShiftAssignmentController(
            ShiftAssignmentPortIn assignmentPortIn,
            ShiftAssignmentApiMapper apiMapper
    ) {
        this.assignmentPortIn = assignmentPortIn;
        this.apiMapper = apiMapper;
    }

    @PostMapping("/shifts/{shiftId}/assignments")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_CREATE_ALL')"
    )
    public ResponseEntity<
            ApiResponse<ShiftAssignmentAdminResponse>
            > create(
            @PathVariable UUID shiftId,
            @RequestBody CreateShiftAssignmentRequest request
    ) {
        ShiftAssignment created =
                assignmentPortIn.createAssignment(
                        shiftId,
                        apiMapper.toDomain(request)
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(
                        "Shift assignment created successfully",
                        apiMapper.toAdminResponse(created)
                )
        );
    }

    @GetMapping("/shifts/{shiftId}/assignments")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_READ_ALL')"
    )
    public ResponseEntity<
            ApiResponse<List<ShiftAssignmentAdminResponse>>
            > getByShift(
            @PathVariable UUID shiftId,
            @RequestParam(required = false)
            ShiftAssignmentStatus status
    ) {
        List<ShiftAssignment> results =
                assignmentPortIn.getAssignmentsByShift(
                        shiftId,
                        status
                );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched shift assignments successfully",
                apiMapper.toAdminResponses(results)
        ));
    }

    @GetMapping("/shift-assignments/me")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_READ_OWN')"
    )
    public ResponseEntity<
            ApiResponse<List<ShiftAssignmentAdminResponse>>
            > getMyAssignments(
            @ModelAttribute MyShiftAssignmentFilterRequest request
    ) {
        List<ShiftAssignment> results =
                assignmentPortIn.getMyAssignments(
                        request.fromDate(),
                        request.toDate(),
                        request.status()
                );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched current employee assignments successfully",
                apiMapper.toAdminResponses(results)
        ));
    }

    @GetMapping("/shift-assignments/{assignmentId}")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_READ_ALL')"
    )
    public ResponseEntity<
            ApiResponse<ShiftAssignmentAdminResponse>
            > getById(
            @PathVariable UUID assignmentId
    ) {
        ShiftAssignment result =
                assignmentPortIn.getAssignmentById(assignmentId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched shift assignment successfully",
                apiMapper.toAdminResponse(result)
        ));
    }

    @GetMapping("/shift-assignments")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_READ_ALL')"
    )
    public ResponseEntity<
            ApiResponse<List<ShiftAssignmentAdminResponse>>
            > getAll(
            @ModelAttribute ShiftAssignmentFilterRequest request
    ) {
        List<ShiftAssignment> results =
                assignmentPortIn.getAssignments(
                        request.parkingLotId(),
                        request.shiftId(),
                        request.employeeId(),
                        request.gateId(),
                        request.status(),
                        request.fromDate(),
                        request.toDate(),
                        request.shiftType()
                );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched shift assignments successfully",
                apiMapper.toAdminResponses(results)
        ));
    }

    @PutMapping("/shift-assignments/{assignmentId}")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_UPDATE_ALL')"
    )
    public ResponseEntity<
            ApiResponse<ShiftAssignmentAdminResponse>
            > update(
            @PathVariable UUID assignmentId,
            @RequestBody UpdateShiftAssignmentRequest request
    ) {
        ShiftAssignment updated =
                assignmentPortIn.updateAssignment(
                        assignmentId,
                        apiMapper.toDomain(request)
                );

        return ResponseEntity.ok(ApiResponse.ok(
                "Shift assignment updated successfully",
                apiMapper.toAdminResponse(updated)
        ));
    }

    @PatchMapping(
            "/shift-assignments/{assignmentId}/replace"
    )
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_UPDATE_ALL')"
    )
    public ResponseEntity<
            ApiResponse<ShiftAssignmentAdminResponse>
            > replace(
            @PathVariable UUID assignmentId,
            @RequestBody ReplaceShiftAssignmentRequest request
    ) {
        ShiftAssignment replacement =
                assignmentPortIn.replaceAssignment(
                        assignmentId,
                        request.replacementEmployeeId(),
                        request.reason()
                );

        return ResponseEntity.ok(ApiResponse.ok(
                "Shift assignment replaced successfully",
                apiMapper.toAdminResponse(replacement)
        ));
    }

    @PostMapping("/shift-assignments/swap")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_UPDATE_ALL')"
    )
    public ResponseEntity<
            ApiResponse<List<ShiftAssignmentAdminResponse>>
            > swap(
            @RequestBody SwapShiftAssignmentRequest request
    ) {
        List<ShiftAssignment> results =
                assignmentPortIn.swapAssignments(
                        request.firstAssignmentId(),
                        request.secondAssignmentId(),
                        request.reason()
                );

        return ResponseEntity.ok(ApiResponse.ok(
                "Shift assignments swapped successfully",
                apiMapper.toAdminResponses(results)
        ));
    }

    @DeleteMapping("/shift-assignments/{assignmentId}")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_DELETE_ALL')"
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID assignmentId
    ) {
        assignmentPortIn.deleteAssignment(assignmentId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Shift assignment removed successfully"
        ));
    }
}