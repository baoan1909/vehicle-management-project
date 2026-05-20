package com.ban.vehicle_management.entrypoint.controller.people;

import com.ban.vehicle_management.application.people.employee.mapper.EmployeeApiMapper;
import com.ban.vehicle_management.application.people.employee.port.in.EmployeePortIn;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.entrypoint.dto.people.employee.request.CreateEmployeeRequest;
import com.ban.vehicle_management.entrypoint.dto.people.employee.request.EmployeeFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.people.employee.request.UpdateEmployeeRequest;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people/employees")
public class EmployeeController {

    private final EmployeePortIn employeePortIn;
    private final EmployeeApiMapper employeeApiMapper;

    public EmployeeController(EmployeePortIn employeePortIn, EmployeeApiMapper employeeApiMapper) {
        this.employeePortIn = employeePortIn;
        this.employeeApiMapper = employeeApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> createEmployee(@RequestBody CreateEmployeeRequest request) {
        Employee createdEmployee = employeePortIn.createEmployee(employeeApiMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Employee created successfully",
                employeeApiMapper.toAdminResponse(createdEmployee)
        ));
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
