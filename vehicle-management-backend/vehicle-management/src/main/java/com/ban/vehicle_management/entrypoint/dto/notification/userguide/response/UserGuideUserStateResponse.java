package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserGuideUserStateResponse {

    private Integer completedVersion;
    private Instant completedAt;
    private Integer dismissedVersion;
    private Instant dismissedAt;
    private Instant startedAt;
    private UUID currentStepId;
    private Instant lastOpenedAt;
}
