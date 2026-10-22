package com.ban.vehicle_management.infrastructure.persistence.database.entity.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_guides", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideEntity extends AuditableEntity {

    @Id
    @Column(name = "guide_id", nullable = false)
    private UUID guideId;

    @Column(name = "feature_id", nullable = false)
    private UUID featureId;

    @Column(name = "code", nullable = false, length = 120)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserGuideStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_type", nullable = false, length = 40)
    private UserGuideDisplayType displayType;

    @Enumerated(EnumType.STRING)
    @Column(name = "replay_policy", nullable = false, length = 40)
    private UserGuideReplayPolicy replayPolicy;

    @Column(name = "context_key", nullable = false, length = 120)
    private String contextKey;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;
}
