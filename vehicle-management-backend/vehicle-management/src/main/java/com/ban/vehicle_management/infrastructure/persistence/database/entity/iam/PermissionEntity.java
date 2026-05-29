package com.ban.vehicle_management.infrastructure.persistence.database.entity.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permissions", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionEntity extends AuditableEntity {

    @Id
    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

    @Column(name = "permission_code", nullable = false, unique = true)
    private String permissionCode;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(name = "action_id", nullable = false)
    private UUID actionId;

    @Column(name = "scope_id", nullable = false)
    private UUID scopeId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "permission")
    private Set<com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RolePermissionEntity> rolePermissions = new HashSet<>();

}


