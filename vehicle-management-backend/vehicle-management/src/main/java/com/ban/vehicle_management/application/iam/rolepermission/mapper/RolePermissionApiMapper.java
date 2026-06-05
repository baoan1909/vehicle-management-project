package com.ban.vehicle_management.application.iam.rolepermission.mapper;

import com.ban.vehicle_management.application.iam.permission.mapper.PermissionApiMapper;
import com.ban.vehicle_management.application.iam.rolepermission.model.command.AssignPermissionsToRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.command.RevokePermissionFromRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;
import com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.request.AssignRolePermissionsRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.response.RolePermissionsResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = PermissionApiMapper.class)
public interface RolePermissionApiMapper {

    @Mapping(target = "roleId", source = "roleId")
    @Mapping(target = "permissionIds", source = "request.permissionIds")
    AssignPermissionsToRoleCommand toAssignCommand(java.util.UUID roleId, AssignRolePermissionsRequest request);

    default RevokePermissionFromRoleCommand toRevokeCommand(java.util.UUID roleId, java.util.UUID permissionId) {
        return new RevokePermissionFromRoleCommand(roleId, permissionId);
    }

    RolePermissionsResponse toResponse(RolePermissionsResult result);
}
