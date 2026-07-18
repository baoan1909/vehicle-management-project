package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.rolepermission.mapper.RolePermissionApiMapper;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.AssignPermissionsToRolePortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.GetRolePermissionAuditLogsPortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.GetRolePermissionsPortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.RevokePermissionFromRolePortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.request.AssignRolePermissionsRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.response.RolePermissionAuditLogResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.response.RolePermissionsResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles/{roleId}/permissions")
public class RolePermissionController {

    private final AssignPermissionsToRolePortIn assignPermissionsToRolePortIn;
    private final RevokePermissionFromRolePortIn revokePermissionFromRolePortIn;
    private final GetRolePermissionsPortIn getRolePermissionsPortIn;
    private final GetRolePermissionAuditLogsPortIn getRolePermissionAuditLogsPortIn;
    private final RolePermissionApiMapper rolePermissionApiMapper;

    public RolePermissionController(
            AssignPermissionsToRolePortIn assignPermissionsToRolePortIn,
            RevokePermissionFromRolePortIn revokePermissionFromRolePortIn,
            GetRolePermissionsPortIn getRolePermissionsPortIn,
            GetRolePermissionAuditLogsPortIn getRolePermissionAuditLogsPortIn,
            RolePermissionApiMapper rolePermissionApiMapper
    ) {
        this.assignPermissionsToRolePortIn = assignPermissionsToRolePortIn;
        this.revokePermissionFromRolePortIn = revokePermissionFromRolePortIn;
        this.getRolePermissionsPortIn = getRolePermissionsPortIn;
        this.getRolePermissionAuditLogsPortIn = getRolePermissionAuditLogsPortIn;
        this.rolePermissionApiMapper = rolePermissionApiMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ_ALL')")
    public ResponseEntity<ApiResponse<RolePermissionsResponse>> getRolePermissions(@PathVariable UUID roleId) {
        RolePermissionsResult result = getRolePermissionsPortIn.getRolePermissions(roleId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched role permissions successfully",
                rolePermissionApiMapper.toResponse(result)
        ));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_ASSIGN_PERMISSION_ALL')")
    public ResponseEntity<ApiResponse<RolePermissionsResponse>> assignPermissionsToRole(
            @PathVariable UUID roleId,
            @RequestBody AssignRolePermissionsRequest request
    ) {
        RolePermissionsResult result = assignPermissionsToRolePortIn.assignPermissionsToRole(
                rolePermissionApiMapper.toAssignCommand(roleId, request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Role permissions synchronized successfully",
                rolePermissionApiMapper.toResponse(result)
        ));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('ROLE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<RolePermissionAuditLogResponse>>> getRolePermissionAuditLogs(
            @PathVariable UUID roleId,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched role permission audit logs successfully",
                rolePermissionApiMapper.toAuditLogResponses(
                        getRolePermissionAuditLogsPortIn.getRolePermissionAuditLogs(roleId, limit)
                )
        ));
    }

    @DeleteMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('ROLE_REVOKE_PERMISSION_ALL')")
    public ResponseEntity<ApiResponse<RolePermissionsResponse>> revokePermissionFromRole(
            @PathVariable UUID roleId,
            @PathVariable UUID permissionId
    ) {
        RolePermissionsResult result = revokePermissionFromRolePortIn.revokePermissionFromRole(
                rolePermissionApiMapper.toRevokeCommand(roleId, permissionId)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Role permission revoked successfully",
                rolePermissionApiMapper.toResponse(result)
        ));
    }
}
