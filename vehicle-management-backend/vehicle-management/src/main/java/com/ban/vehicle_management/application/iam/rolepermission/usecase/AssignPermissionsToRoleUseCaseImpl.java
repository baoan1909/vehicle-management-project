package com.ban.vehicle_management.application.iam.rolepermission.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.iam.rolepermission.model.command.AssignPermissionsToRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.AssignPermissionsToRolePortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.GetRolePermissionsPortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.out.RolePermissionPortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
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

    private static final String AUDIT_ACTION_SYNC = "ROLE_PERMISSION_SYNC";

    private final RolePortOut rolePortOut;
    private final PermissionPortOut permissionPortOut;
    private final RolePermissionPortOut rolePermissionPortOut;
    private final GetRolePermissionsPortIn getRolePermissionsPortIn;
    private final AuditLogPortOut auditLogPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;

    public AssignPermissionsToRoleUseCaseImpl(
            RolePortOut rolePortOut,
            PermissionPortOut permissionPortOut,
            RolePermissionPortOut rolePermissionPortOut,
            GetRolePermissionsPortIn getRolePermissionsPortIn,
            AuditLogPortOut auditLogPortOut,
            CurrentAccountPortIn currentAccountPortIn
    ) {
        this.rolePortOut = rolePortOut;
        this.permissionPortOut = permissionPortOut;
        this.rolePermissionPortOut = rolePermissionPortOut;
        this.getRolePermissionsPortIn = getRolePermissionsPortIn;
        this.auditLogPortOut = auditLogPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
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
        Set<UUID> previousActivePermissionIds = activePermissionIds(existingMappings);
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

        RolePermissionsResult result = getRolePermissionsPortIn.getRolePermissions(role.getRoleId());

        if (!changedMappings.isEmpty()) {
            writeSyncAuditLog(role, previousActivePermissionIds, result.permissions());
        }

        return result;
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

    private Set<UUID> activePermissionIds(List<RolePermission> mappings) {
        Set<UUID> activeIds = new LinkedHashSet<>();

        for (RolePermission mapping : mappings) {
            if (Boolean.TRUE.equals(mapping.getIsActive())) {
                activeIds.add(mapping.getPermissionId());
            }
        }

        return activeIds;
    }

    private void writeSyncAuditLog(Role role, Set<UUID> previousActivePermissionIds, List<Permission> nextPermissions) {
        Map<UUID, String> previousPermissionCodes = permissionCodesById(previousActivePermissionIds);
        Map<UUID, String> nextPermissionCodes = permissionCodesById(
                nextPermissions.stream()
                        .map(Permission::getPermissionId)
                        .collect(LinkedHashSet::new, Set::add, Set::addAll)
        );

        Set<String> previousCodes = new LinkedHashSet<>(previousPermissionCodes.values());
        Set<String> nextCodes = new LinkedHashSet<>(nextPermissionCodes.values());
        Set<String> addedCodes = difference(nextCodes, previousCodes);
        Set<String> removedCodes = difference(previousCodes, nextCodes);

        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(currentAccountPortIn.getCurrentAccountId().orElse(null));
        auditLog.setAction(AUDIT_ACTION_SYNC);
        auditLog.setTargetSchema("iam");
        auditLog.setTargetTable("role_permissions");
        auditLog.setTargetId(role.getRoleId());
        auditLog.setOldData(Map.of(
                "roleCode", role.getCode(),
                "permissionCodes", previousCodes
        ));
        auditLog.setNewData(Map.of(
                "roleCode", role.getCode(),
                "permissionCodes", nextCodes,
                "addedPermissionCodes", addedCodes,
                "removedPermissionCodes", removedCodes
        ));

        auditLogPortOut.save(auditLog);
    }

    private Map<UUID, String> permissionCodesById(Set<UUID> permissionIds) {
        Map<UUID, String> permissionCodes = new LinkedHashMap<>();

        if (permissionIds.isEmpty()) {
            return permissionCodes;
        }

        permissionPortOut.findByIds(permissionIds).forEach(permission ->
                permissionCodes.put(permission.getPermissionId(), permission.getPermissionCode())
        );

        return permissionCodes;
    }

    private Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new LinkedHashSet<>(left);
        result.removeAll(right);
        return result;
    }
}
