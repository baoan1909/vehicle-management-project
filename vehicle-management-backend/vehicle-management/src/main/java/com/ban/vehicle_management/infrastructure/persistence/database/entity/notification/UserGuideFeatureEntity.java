package com.ban.vehicle_management.infrastructure.persistence.database.entity.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
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
@Table(name = "user_guide_features", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideFeatureEntity extends AuditableEntity {

    @Id
    @Column(name = "feature_id", nullable = false)
    private UUID featureId;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;
}
