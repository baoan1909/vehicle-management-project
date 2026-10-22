package com.ban.vehicle_management.domain.notification.userguide.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideUserState extends AuditableDomainModel {

    private UUID userGuideStateId;
    private UUID accountId;
    private UUID guideId;
    private Integer completedVersion;
    private Instant completedAt;
    private Integer dismissedVersion;
    private Instant dismissedAt;
    private Instant lastOpenedAt;
    private Instant startedAt;
    private UUID currentStepId;
}
