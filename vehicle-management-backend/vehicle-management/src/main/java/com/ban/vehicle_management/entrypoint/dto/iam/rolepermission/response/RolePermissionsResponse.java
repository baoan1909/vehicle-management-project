package com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.response;

import com.ban.vehicle_management.entrypoint.dto.iam.permission.response.PermissionAdminResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RolePermissionsResponse {
    private UUID roleId;
    private String roleCode;
    private String roleName;
    private Boolean isSystem;
    private Boolean isActive;
    private List<PermissionAdminResponse> permissions;
}
