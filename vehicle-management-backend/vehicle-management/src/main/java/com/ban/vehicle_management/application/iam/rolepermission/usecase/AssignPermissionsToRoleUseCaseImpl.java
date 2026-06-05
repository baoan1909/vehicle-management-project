package com.ban.vehicle_management.application.iam.rolepermission.usecase;

import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.iam.rolepermission.model.command.AssignPermissionsToRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.AssignPermissionsToRolePortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.GetRolePermissionsPortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.out.RolePermissionPortOut;
import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.domain.iam.role.model.RolePermission;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AssignPermissionsToRoleUseCaseImpl implements AssignPermissionsToRolePortIn {

    private final RolePortOut rolePortOut;
    private final PermissionPortOut permissionPortOut;
    private final RolePermissionPortOut rolePermissionPortOut;
    private final GetRolePermissionsPortIn getRolePermissionsPortIn;

    public AssignPermissionsToRoleUseCaseImpl(
            RolePortOut rolePortOut,
            PermissionPortOut permissionPortOut,
            RolePermissionPortOut rolePermissionPortOut,
            GetRolePermissionsPortIn getRolePermissionsPortIn
    ) {
        this.rolePortOut = rolePortOut;
        this.permissionPortOut = permissionPortOut;
        this.rolePermissionPortOut = rolePermissionPortOut;
        this.getRolePermissionsPortIn = getRolePermissionsPortIn;
    }

    @Override
    @Transactional
    public RolePermissionsResult assignPermissionsToRole(AssignPermissionsToRoleCommand command) {
        Role role = rolePortOut.findById(command.roleId())
                .orElseThrow(() -> new NotFoundException("Role not found"));
        ensureRoleCanBeManaged(role);

        Set<UUID> requestedPermissionIds = normalizePermissionIds(command.permissionIds());
        validatePermissionsExist(requestedPermissionIds);

        List<RolePermission> existingMappings = rolePermissionPortOut.findByRoleId(role.getRoleId());
        Map<UUID, RolePermission> mappingsByPermissionId = mapByPermissionId(existingMappings);
        List<RolePermission> changedMappings = new ArrayList<>();

        for (RolePermission mapping : existingMappings) {
            if (Boolean.TRUE.equals(mapping.getIsSystem())) {
                continue;
            }

            boolean shouldBeActive = requestedPermissionIds.contains(mapping.getPermissionId());
            if (!Boolean.valueOf(shouldBeActive).equals(mapping.getIsActive())) {
                mapping.setIsActive(shouldBeActive);
                changedMappings.add(mapping);
            }
        }

        for (UUID permissionId : requestedPermissionIds) {
            RolePermission existingMapping = mappingsByPermissionId.get(permissionId);
            if (existingMapping != null) {
                continue;
            }

            RolePermission newMapping = new RolePermission();
            newMapping.setId(UUID.randomUUID());
            newMapping.setRoleId(role.getRoleId());
            newMapping.setPermissionId(permissionId);
            newMapping.setIsActive(true);
            newMapping.setIsSystem(false);
            changedMappings.add(newMapping);
        }

        if (!changedMappings.isEmpty()) {
            rolePermissionPortOut.saveAll(changedMappings);
        }

        return getRolePermissionsPortIn.getRolePermissions(role.getRoleId());
    }

    private void ensureRoleCanBeManaged(Role role) {
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BadRequestException("System role permissions cannot be modified");
        }
    }

    private Set<UUID> normalizePermissionIds(Collection<UUID> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return Set.of();
        }

        Set<UUID> normalizedIds = new LinkedHashSet<>();
        for (UUID permissionId : permissionIds) {
            if (permissionId == null) {
                throw new BadRequestException("permissionIds must not contain null values");
            }
            normalizedIds.add(permissionId);
        }
        return normalizedIds;
    }

    private void validatePermissionsExist(Set<UUID> permissionIds) {
        if (permissionIds.isEmpty()) {
            return;
        }

        List<Permission> permissions = permissionPortOut.findByIds(permissionIds);
        if (permissions.size() == permissionIds.size()) {
            return;
        }

        Set<UUID> existingIds = permissions.stream()
                .map(Permission::getPermissionId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        List<UUID> missingIds = permissionIds.stream()
                .filter(permissionId -> !existingIds.contains(permissionId))
                .toList();
        throw new NotFoundException("Permissions not found: " + missingIds);
    }

    private Map<UUID, RolePermission> mapByPermissionId(List<RolePermission> existingMappings) {
        Map<UUID, RolePermission> mappings = new LinkedHashMap<>();
        for (RolePermission mapping : existingMappings) {
            mappings.put(mapping.getPermissionId(), mapping);
        }
        return mappings;
    }
}
