package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.employeerosterrule.mapper.EmployeeRosterRuleApiMapper;
import com.ban.vehicle_management.application.operations.employeerosterrule.port.in.EmployeeRosterRulePortIn;
import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.entrypoint.dto.operations.employeerosterrule.request.CreateEmployeeRosterRuleRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.employeerosterrule.request.EmployeeRosterRuleFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.employeerosterrule.request.UpdateEmployeeRosterRuleRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.employeerosterrule.response.EmployeeRosterRuleAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations/employee-roster-rules")
public class EmployeeRosterRuleController {

    private final EmployeeRosterRulePortIn rosterRulePortIn;
    private final EmployeeRosterRuleApiMapper apiMapper;

    public EmployeeRosterRuleController(
            EmployeeRosterRulePortIn rosterRulePortIn,
            EmployeeRosterRuleApiMapper apiMapper
    ) {
        this.rosterRulePortIn = rosterRulePortIn;
        this.apiMapper = apiMapper;
    }

    @PostMapping
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_CREATE_ALL')"
    )
    public ResponseEntity<ApiResponse<EmployeeRosterRuleAdminResponse>> create(
            @RequestBody CreateEmployeeRosterRuleRequest request
    ) {
        EmployeeRosterRule created = rosterRulePortIn.createRule(
                apiMapper.toDomain(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(
                        "Employee roster rule created successfully",
                        apiMapper.toAdminResponse(created)
                )
        );
    }

    @GetMapping("/{rosterRuleId}")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_READ_ALL')"
    )
    public ResponseEntity<ApiResponse<EmployeeRosterRuleAdminResponse>> getById(
            @PathVariable UUID rosterRuleId
    ) {
        EmployeeRosterRule result =
                rosterRulePortIn.getRuleById(rosterRuleId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched employee roster rule successfully",
                apiMapper.toAdminResponse(result)
        ));
    }

    @GetMapping
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_READ_ALL')"
    )
    public ResponseEntity<
            ApiResponse<List<EmployeeRosterRuleAdminResponse>>
            > getAll(
            @ModelAttribute EmployeeRosterRuleFilterRequest request
    ) {
        List<EmployeeRosterRule> results =
                rosterRulePortIn.getRules(
                        request.parkingLotId(),
                        request.employeeId(),
                        request.preferredShiftType(),
                        request.preferredGateId(),
                        request.weeklyDayOff(),
                        request.assignmentMode(),
                        request.status(),
                        request.effectiveDate()
                );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched employee roster rules successfully",
                apiMapper.toAdminResponses(results)
        ));
    }

    @PutMapping("/{rosterRuleId}")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_UPDATE_ALL')"
    )
    public ResponseEntity<ApiResponse<EmployeeRosterRuleAdminResponse>> update(
            @PathVariable UUID rosterRuleId,
            @RequestBody UpdateEmployeeRosterRuleRequest request
    ) {
        EmployeeRosterRule updated = rosterRulePortIn.updateRule(
                rosterRuleId,
                apiMapper.toDomain(request)
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Employee roster rule updated successfully",
                apiMapper.toAdminResponse(updated)
        ));
    }

    @PatchMapping("/{rosterRuleId}/activate")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_UPDATE_ALL')"
    )
    public ResponseEntity<ApiResponse<EmployeeRosterRuleAdminResponse>> activate(
            @PathVariable UUID rosterRuleId
    ) {
        EmployeeRosterRule activated =
                rosterRulePortIn.activateRule(rosterRuleId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Employee roster rule activated successfully",
                apiMapper.toAdminResponse(activated)
        ));
    }

    @DeleteMapping("/{rosterRuleId}")
    @PreAuthorize(
            "@permissionAuthorizer.hasPermission(" +
                    "'SHIFT_ASSIGNMENT_DELETE_ALL')"
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID rosterRuleId
    ) {
        rosterRulePortIn.deleteRule(rosterRuleId);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Employee roster rule deactivated successfully"
                )
        );
    }
}