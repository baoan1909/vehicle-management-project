package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.role.mapper.RoleApiMapper;
import com.ban.vehicle_management.application.iam.role.port.in.RolePortIn;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.entrypoint.dto.iam.role.request.CreateRoleRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.role.request.UpdateRoleRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.role.response.RoleAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/iam/roles")
public class RoleController {
    private final RolePortIn rolePortIn;
    private final RoleApiMapper roleApiMapper;

    public RoleController(RolePortIn rolePortIn, RoleApiMapper roleApiMapper) {
        this.rolePortIn = rolePortIn;
        this.roleApiMapper = roleApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleAdminResponse>> createRole(@RequestBody CreateRoleRequest request) {
        Role createdRole = rolePortIn.createRole(roleApiMapper.toDomain(request));
        RoleAdminResponse response = roleApiMapper.toAdminResponse(createdRole);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Role created successfully", response));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleAdminResponse>> updateRole(
            @PathVariable UUID roleId,
            @RequestBody UpdateRoleRequest request
    ) {
        Role updatedRole = rolePortIn.updateRole(roleId, roleApiMapper.toDomain(request));
        RoleAdminResponse response = roleApiMapper.toAdminResponse(updatedRole);

        return ResponseEntity.ok(ApiResponse.ok("Role updated successfully", response));
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleAdminResponse>> getRoleById(@PathVariable UUID roleId) {
        Role role = rolePortIn.getRoleById(roleId);
        RoleAdminResponse response = roleApiMapper.toAdminResponse(role);

        return ResponseEntity.ok(ApiResponse.ok("Fetched role successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleAdminResponse>>> getRoles(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isSystem,
            @RequestParam(required = false) String keyword
    ) {
        List<Role> roles = rolePortIn.getRoles(isActive, isSystem, keyword);
        List<RoleAdminResponse> response = roleApiMapper.toAdminResponses(roles);

        return ResponseEntity.ok(ApiResponse.ok("Fetched roles successfully", response));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID roleId) {
        rolePortIn.deleteRole(roleId);
        return ResponseEntity.ok(ApiResponse.ok("Role deactivated successfully"));
    }

}
