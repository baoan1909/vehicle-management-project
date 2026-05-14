package com.ban.vehicle_management.infrastructure.persistence.iam.permission;

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

    @Column(name = "module", nullable = false)
    private String module;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

}
