package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.organization.mapper.ParkingLotAssignmentApiMapper;
import com.ban.vehicle_management.application.iam.organization.port.in.ParkingLotAssignmentPortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.CreateEmployeeParkingLotAssignmentRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.CreateParkingLotManagementAssignmentRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.ReplaceParkingLotManagerRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.TransferEmployeeParkingLotRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.SuspendParkingLotOperationsRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.response.MyParkingLotScopeResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.response.ParkingLotManagementAssignmentAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeParkingLotAssignmentAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iam/parking-lot-assignments")
public class ParkingLotAssignmentController {
    private final ParkingLotAssignmentPortIn useCase;
    private final ParkingLotAssignmentApiMapper mapper;

    public ParkingLotAssignmentController(ParkingLotAssignmentPortIn useCase, ParkingLotAssignmentApiMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @PostMapping("/organizations/{organizationId}/parking-lots/{parkingLotId}/managers")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_MEMBERSHIP_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<ParkingLotManagementAssignmentAdminResponse>> assignManager(
            @PathVariable UUID organizationId,
            @PathVariable UUID parkingLotId,
            @RequestBody CreateParkingLotManagementAssignmentRequest request) {
        var result = useCase.assignManager(
                organizationId, parkingLotId, request.managerAccountId(), request.managementRole(),
                request.effectiveAt(), request.acceptanceRequired(), request.handoverNote());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Parking Manager assigned successfully", mapper.toAdminResponse(result)));
    }

    @PostMapping("/organizations/{organizationId}/parking-lots/{parkingLotId}/managers/replace")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_MEMBERSHIP_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<ParkingLotManagementAssignmentAdminResponse>> replaceManager(
            @PathVariable UUID organizationId,
            @PathVariable UUID parkingLotId,
            @RequestBody ReplaceParkingLotManagerRequest request) {
        var result = useCase.replaceManager(
                organizationId, parkingLotId, request.outgoingManagerId(), request.incomingManagerId(),
                request.effectiveAt(), request.acceptanceRequired(), request.handoverNote());
        return ResponseEntity.ok(ApiResponse.ok("Parking Manager replacement scheduled", mapper.toAdminResponse(result)));
    }

    @PostMapping("/management/{assignmentId}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ParkingLotManagementAssignmentAdminResponse>> acceptManagerHandover(
            @PathVariable UUID assignmentId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Parking Manager handover accepted",
                mapper.toAdminResponse(useCase.acceptManagementAssignment(assignmentId))));
    }

    @GetMapping("/organizations/{organizationId}/parking-lots/{parkingLotId}/management-history")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_MEMBERSHIP_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingLotManagementAssignmentAdminResponse>>> getHistory(
            @PathVariable UUID organizationId, @PathVariable UUID parkingLotId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking-lot management history",
                mapper.toAdminResponses(useCase.getManagementHistory(organizationId, parkingLotId))));
    }

    @PostMapping("/organizations/{organizationId}/parking-lots/{parkingLotId}/employees")
    @PreAuthorize("@permissionAuthorizer.hasPermission('EMPLOYEE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<EmployeeParkingLotAssignmentAdminResponse>> assignEmployee(
            @PathVariable UUID organizationId,
            @PathVariable UUID parkingLotId,
            @RequestBody CreateEmployeeParkingLotAssignmentRequest request) {
        var result = useCase.assignEmployee(
                organizationId, parkingLotId, request.employeeId(), request.assignmentType(),
                request.effectiveFrom(), request.effectiveTo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Employee assigned to parking lot", mapper.toAdminResponse(result)));
    }

    @GetMapping("/organizations/{organizationId}/employees")
    @PreAuthorize("@permissionAuthorizer.hasPermission('EMPLOYEE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<EmployeeParkingLotAssignmentAdminResponse>>> getEmployeeAssignments(
            @PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched Employee parking-lot assignments",
                mapper.toEmployeeAdminResponses(useCase.getEmployeeAssignments(organizationId))));
    }

    @PostMapping("/organizations/{organizationId}/employees/transfer")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_MEMBERSHIP_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<EmployeeParkingLotAssignmentAdminResponse>> transferEmployee(
            @PathVariable UUID organizationId,
            @RequestBody TransferEmployeeParkingLotRequest request) {
        var result = useCase.transferEmployee(
                organizationId, request.employeeId(), request.sourceParkingLotId(),
                request.targetParkingLotId(), request.effectiveAt(), request.reason());
        return ResponseEntity.ok(ApiResponse.ok("Employee transferred between parking lots", mapper.toAdminResponse(result)));
    }

    @GetMapping("/me/parking-lots")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SCOPE_ASSIGNED')")
    public ResponseEntity<ApiResponse<MyParkingLotScopeResponse>> getMyParkingLots() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched current parking-lot scope",
                new MyParkingLotScopeResponse(useCase.getMyReadableParkingLots(), useCase.getMyOperableParkingLots())));
    }

    @PostMapping("/organizations/{organizationId}/parking-lots/{parkingLotId}/operations/suspend")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_MEMBERSHIP_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> suspendOperations(
            @PathVariable UUID organizationId,
            @PathVariable UUID parkingLotId,
            @RequestBody SuspendParkingLotOperationsRequest request) {
        useCase.suspendParkingLotOperations(organizationId, parkingLotId, request.reason());
        return ResponseEntity.ok(ApiResponse.ok("Parking-lot operations suspended"));
    }

    @PostMapping("/organizations/{organizationId}/parking-lots/{parkingLotId}/operations/resume")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_MEMBERSHIP_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> resumeOperations(
            @PathVariable UUID organizationId,
            @PathVariable UUID parkingLotId) {
        useCase.resumeParkingLotOperations(organizationId, parkingLotId);
        return ResponseEntity.ok(ApiResponse.ok("Parking-lot operations resumed"));
    }
}
