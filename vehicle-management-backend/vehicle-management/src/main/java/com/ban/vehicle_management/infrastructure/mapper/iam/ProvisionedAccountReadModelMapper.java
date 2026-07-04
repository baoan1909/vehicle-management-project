package com.ban.vehicle_management.infrastructure.mapper.iam;

import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.PermissionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RolePermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProvisionedAccountReadModelMapper {

    default ProvisionedAccountResult toResult(AccountEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProvisionedAccountResult(
                toAccountInfoResult(entity),
                toRoleInfoResult(entity.getRole())
        );
    }

    default ProvisionedAccountResult toSummaryResult(AccountEntity entity) {
        return toResult(entity);
    }

    @Mapping(target = "accountStatus", source = "status")
    ProvisionedAccountResult.AccountInfoResult toAccountInfoResult(AccountEntity entity);

    @Mapping(target = "roleCode", source = "code")
    @Mapping(target = "roleName", source = "name")
    @Mapping(target = "permissionCodes", expression = "java(mapPermissionCodes(role))")
    ProvisionedAccountResult.RoleInfoResult toRoleInfoResult(RoleEntity role);

    default List<String> mapPermissionCodes(RoleEntity role) {
        if (role == null || role.getRolePermissions() == null) {
            return List.of();
        }
        return role.getRolePermissions().stream()
                .filter(RolePermissionEntity::getIsActive)
                .map(RolePermissionEntity::getPermission)
                .filter(permission -> permission != null && permission.getPermissionCode() != null)
                .map(PermissionEntity::getPermissionCode)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
