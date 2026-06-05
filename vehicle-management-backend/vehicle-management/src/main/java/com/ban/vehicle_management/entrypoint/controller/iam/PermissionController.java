package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.permission.mapper.PermissionApiMapper;
import com.ban.vehicle_management.application.iam.permission.port.in.PermissionPortIn;
import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.entrypoint.dto.iam.permission.request.PermissionFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.permission.response.PermissionAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionPortIn permissionPortIn;
    private final PermissionApiMapper permissionApiMapper;

    public PermissionController(
            PermissionPortIn permissionPortIn,
            PermissionApiMapper permissionApiMapper
    ) {
        this.permissionPortIn = permissionPortIn;
        this.permissionApiMapper = permissionApiMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_READ_ALL')")
    public ResponseEntity<ApiResponse<List<PermissionAdminResponse>>> getPermissions(
            @ModelAttribute PermissionFilterRequest request
    ) {
        List<Permission> permissions = permissionPortIn.getPermissions(request.keyword());
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched permissions successfully",
                permissionApiMapper.toAdminResponses(permissions)
        ));
    }
}
