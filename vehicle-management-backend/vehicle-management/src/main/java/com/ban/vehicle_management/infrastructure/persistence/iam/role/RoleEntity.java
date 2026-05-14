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
@Table(name = "roles", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity extends AuditableEntity {

    @Id
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

}
