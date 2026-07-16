package com.ban.vehicle_management.entrypoint.controller.people;

import com.ban.vehicle_management.application.people.employee.mapper.EmployeeApiMapper;
import com.ban.vehicle_management.application.people.employee.mapper.EmployeeManagerReadApiMapper;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeActivityTimelineResult;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeRecentShiftResult;
import com.ban.vehicle_management.application.people.employee.port.in.EmployeeManagerReadPortIn;
import com.ban.vehicle_management.application.people.employee.port.in.EmployeePortIn;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.entrypoint.dto.people.employee.request.EmployeeFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.people.employee.request.UpdateEmployeeAdminProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.employee.request.UpdateEmployeeRequest;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeActivityTimelineResponse;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeRecentShiftResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/people/employees")
public class EmployeeController {

    private final EmployeePortIn employeePortIn;
    private final EmployeeManagerReadPortIn employeeManagerReadPortIn;
    private final EmployeeApiMapper employeeApiMapper;
    private final EmployeeManagerReadApiMapper employeeManagerReadApiMapper;

    public EmployeeController(
            EmployeePortIn employeePortIn,
            EmployeeManagerReadPortIn employeeManagerReadPortIn,
            EmployeeApiMapper employeeApiMapper,
            EmployeeManagerReadApiMapper employeeManagerReadApiMapper
    ) {
        this.employeePortIn = employeePortIn;
        this.employeeManagerReadPortIn = employeeManagerReadPortIn;
        this.employeeApiMapper = employeeApiMapper;
        this.employeeManagerReadApiMapper = employeeManagerReadApiMapper;
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> getEmployeeById(@PathVariable UUID employeeId) {
        Employee employee = employeePortIn.getEmployeeById(employeeId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched employee successfully",
                employeeApiMapper.toAdminResponse(employee)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeAdminResponse>>> getEmployees(
            @ModelAttribute EmployeeFilterRequest request
    ) {
        List<Employee> employees = employeePortIn.getEmployees(request.status(), request.keyword());
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched employees successfully",
                employeeApiMapper.toAdminResponses(employees)
        ));
    }

    @GetMapping("/{employeeId}/recent-shifts")
    public ResponseEntity<ApiResponse<List<EmployeeRecentShiftResponse>>> getRecentShifts(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer limit
    ) {
        List<EmployeeRecentShiftResult> results = employeeManagerReadPortIn.getRecentShifts(employeeId, limit);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched recent employee shifts successfully",
                employeeManagerReadApiMapper.toRecentShiftResponses(results)
        ));
    }

    @GetMapping("/{employeeId}/activity-timeline")
    public ResponseEntity<ApiResponse<List<EmployeeActivityTimelineResponse>>> getActivityTimeline(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer limit
    ) {
        List<EmployeeActivityTimelineResult> results = employeeManagerReadPortIn.getActivityTimeline(employeeId, limit);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched employee activity timeline successfully",
                employeeManagerReadApiMapper.toActivityTimelineResponses(results)
        ));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> updateEmployee(
            @PathVariable UUID employeeId,
            @RequestBody UpdateEmployeeRequest request
    ) {
        Employee updatedEmployee = employeePortIn.updateEmployee(employeeId, employeeApiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee updated successfully",
                employeeApiMapper.toAdminResponse(updatedEmployee)
        ));
    }

    @PutMapping("/{employeeId}/profile")
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> updateEmployeeAdminProfile(
            @PathVariable UUID employeeId,
            @RequestBody UpdateEmployeeAdminProfileRequest request
    ) {
        Employee updatedEmployee = employeePortIn.updateEmployeeAdminProfile(
                employeeId,
                employeeApiMapper.toUpdateAdminProfileCommand(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee profile updated successfully",
                employeeApiMapper.toAdminResponse(updatedEmployee)
        ));
    }

    @PostMapping(value = "/{employeeId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasPermission('EMPLOYEE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> uploadEmployeeAvatar(
            @PathVariable UUID employeeId,
            @RequestPart("file") MultipartFile file
    ) {
        Employee employee = employeePortIn.uploadEmployeeAvatar(employeeId, file);
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee avatar updated successfully",
                employeeApiMapper.toAdminResponse(employee)
        ));
    }

    @DeleteMapping("/{employeeId}/avatar")
    @PreAuthorize("@permissionAuthorizer.hasPermission('EMPLOYEE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> deleteEmployeeAvatar(@PathVariable UUID employeeId) {
        Employee employee = employeePortIn.deleteEmployeeAvatar(employeeId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee avatar deleted successfully",
                employeeApiMapper.toAdminResponse(employee)
        ));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID employeeId) {
        employeePortIn.deleteEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.ok("Employee inactivated successfully"));
    }

    @PatchMapping("/{employeeId}/activate")
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> activateEmployee(@PathVariable UUID employeeId) {
        Employee employee = employeePortIn.activateEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee activated successfully",
                employeeApiMapper.toAdminResponse(employee)
        ));
    }

    @PatchMapping("/{employeeId}/inactivate")
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> inactivateEmployee(@PathVariable UUID employeeId) {
        Employee employee = employeePortIn.inactivateEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee inactivated successfully",
                employeeApiMapper.toAdminResponse(employee)
        ));
    }

    @PatchMapping("/{employeeId}/suspend")
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> suspendEmployee(@PathVariable UUID employeeId) {
        Employee employee = employeePortIn.suspendEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee suspended successfully",
                employeeApiMapper.toAdminResponse(employee)
        ));
    }
}
