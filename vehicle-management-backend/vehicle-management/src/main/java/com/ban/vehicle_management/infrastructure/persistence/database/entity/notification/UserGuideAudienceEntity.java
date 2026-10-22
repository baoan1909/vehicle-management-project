package com.ban.vehicle_management.infrastructure.persistence.database.entity.notification;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_guide_audiences", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideAudienceEntity {

    @Id
    @Column(name = "audience_id", nullable = false)
    private UUID audienceId;

    @Column(name = "guide_version_id", nullable = false)
    private UUID guideVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private UserGuideAudienceTargetType targetType;

    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "permission_id")
    private UUID permissionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;
}
