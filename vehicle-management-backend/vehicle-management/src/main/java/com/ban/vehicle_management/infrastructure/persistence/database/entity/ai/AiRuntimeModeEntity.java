package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import com.ban.vehicle_management.shared.enumeration.ai.AiRuntimeMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "runtime_mode", schema = "ai")
@Getter
@Setter
public class AiRuntimeModeEntity {
    @Id
    @Column(name = "singleton_id", nullable = false)
    private Short singletonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private AiRuntimeMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_mode", nullable = false, length = 20)
    private AiRuntimeMode targetMode;

    @Column(name = "epoch", nullable = false)
    private Long epoch;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
