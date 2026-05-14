package com.ban.vehicle_management.infrastructure.persistence.iam.role;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_permissions", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionEntity extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

}
