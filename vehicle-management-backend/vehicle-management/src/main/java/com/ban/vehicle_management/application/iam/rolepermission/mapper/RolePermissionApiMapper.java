package com.ban.vehicle_management.application.iam.rolepermission.mapper;

import com.ban.vehicle_management.application.iam.permission.mapper.PermissionApiMapper;
import com.ban.vehicle_management.application.iam.rolepermission.model.command.AssignPermissionsToRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.command.RevokePermissionFromRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionAuditLogResult;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;
import com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.request.AssignRolePermissionsRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.response.RolePermissionAuditLogResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.response.RolePermissionsResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring", uses = PermissionApiMapper.class)
public interface RolePermissionApiMapper {

    @Mapping(target = "roleId", source = "roleId")
    @Mapping(target = "permissionIds", source = "request.permissionIds")
    AssignPermissionsToRoleCommand toAssignCommand(java.util.UUID roleId, AssignRolePermissionsRequest request);

    default RevokePermissionFromRoleCommand toRevokeCommand(java.util.UUID roleId, java.util.UUID permissionId) {
        return new RevokePermissionFromRoleCommand(roleId, permissionId);
    }

    RolePermissionsResponse toResponse(RolePermissionsResult result);

    @Mapping(target = "eventTime", source = "eventTime", qualifiedByName = "formatVietnamInstant")
    RolePermissionAuditLogResponse toAuditLogResponse(RolePermissionAuditLogResult result);

    List<RolePermissionAuditLogResponse> toAuditLogResponses(List<RolePermissionAuditLogResult> results);

    @Named("formatVietnamInstant")
    default String formatVietnamInstant(Instant value) {
        return DateTimeUtils.formatInstant(value, DateTimeUtils.VIETNAM_ZONE);
    }
}
