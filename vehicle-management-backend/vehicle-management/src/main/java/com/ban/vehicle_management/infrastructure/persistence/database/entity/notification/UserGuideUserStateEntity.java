package com.ban.vehicle_management.infrastructure.persistence.database.entity.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_guide_user_states", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideUserStateEntity extends AuditableEntity {

    @Id
    @Column(name = "user_guide_state_id", nullable = false)
    private UUID userGuideStateId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "guide_id", nullable = false)
    private UUID guideId;

    @Column(name = "completed_version")
    private Integer completedVersion;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "dismissed_version")
    private Integer dismissedVersion;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    @Column(name = "last_opened_at")
    private Instant lastOpenedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "current_step_id")
    private UUID currentStepId;
}
